(ns serveur.actionability
  (:require [serveur.kafka :as kafka]
            [serveur.neo4j :as neo]
            [cheshire.core :as json]
            [clojure.tools.logging :as log]
            [clojure.pprint :refer [pprint]]))

(def agents-root "https://search.clinicalgenome.org/kb/agents/")

(def iri-prefixes {:omim "http://purl.obolibrary.org/obo/OMIM_"
                   :local "http://search.clinicalgenome.org/kb/entity/"})

(def act-iris {:top "http://datamodel.clinicalgenome.org/terms/CG_000082"
               :interv "http://datamodel.clinicalgenome.org/terms/CG_000081"
               :efficacy {0 "http://datamodel.clinicalgenome.org/terms/CG_000061"
                          1 "http://datamodel.clinicalgenome.org/terms/CG_000060"
                          2 "http://datamodel.clinicalgenome.org/terms/CG_000059"
                          3 "http://datamodel.clinicalgenome.org/terms/CG_000058"}
               :evidence {"E" "http://datamodel.clinicalgenome.org/terms/CG_000057"
                          "N" "http://datamodel.clinicalgenome.org/terms/CG_000057"
                          "D" "http://datamodel.clinicalgenome.org/terms/CG_000056"
                          "C" "http://datamodel.clinicalgenome.org/terms/CG_000055"
                          "B" "http://datamodel.clinicalgenome.org/terms/CG_000054"
                          "A" "http://datamodel.clinicalgenome.org/terms/CG_000053"}
               :likelihood {0 "http://datamodel.clinicalgenome.org/terms/CG_000051"
                            1 "http://datamodel.clinicalgenome.org/terms/CG_000050"
                            2 "http://datamodel.clinicalgenome.org/terms/CG_000049"
                            3 "http://datamodel.clinicalgenome.org/terms/CG_000048"}
               :severity {0 "http://datamodel.clinicalgenome.org/terms/CG_000046"
                          1 "http://datamodel.clinicalgenome.org/terms/CG_000045"
                          2 "http://datamodel.clinicalgenome.org/terms/CG_000044"
                          3 "http://datamodel.clinicalgenome.org/terms/CG_000043"}
               :safety {1 "http://datamodel.clinicalgenome.org/terms/CG_000089"
                        2 "http://datamodel.clinicalgenome.org/terms/CG_000088"
                        0 "http://datamodel.clinicalgenome.org/terms/CG_000090"
                        3 "http://datamodel.clinicalgenome.org/terms/CG_000087"}})

(defn generate-local-iri
  "Generate a uuid-based local iri for a new entity"
  []
  (str (:local iri-prefixes) (str (java.util.UUID/randomUUID))))

(defn get-act-score-iri
  "Get the appropriate iri(s), given a score and a category"
  [category score]
;;  (println "get-act-score-iri " score)
  (if  (< 1 (count score))
    [((category act-iris) (read-string (subs score 0 1)))
     ((:evidence act-iris) (subs score 1 2))]
    [((category act-iris) (read-string (subs score 0 1)))]))

(defn import-actionability-score
  "import subscore for actionability curation"
  [score category parent-iri session]
  (let [iri (generate-local-iri)
        scores (get-act-score-iri category score)]
;;    (pprint scores)
    (when (first scores) ;; Only record computable scores--consider logging error otherwise
      (.run session "match (top:Assertion {iri: $parent}) match (str:RDFClass {iri: $score}) merge (s:ActionabilityScore:Assertion:Entity {iri: $iri}) merge (s)-[:was_generated_by]->(top) merge (s)-[:has_subject]->(top) merge (s)-[:has_predicate]->(str)"
            {"parent" parent-iri, "iri" iri, "score" (first scores)})
      (when (second scores) ;; record evidence strength when available
        (.run session "match (s:ActionabilityScore {iri: $iri}) match (e:RDFClass {iri: $score}) merge (s)-[:has_evidence_strength]->(e)"
              {"iri" iri, "score" (second scores)})))))

(defn import-actionability-intervention
  "Called by intervention-actionability-outcome. Import each intervention into neo4j"
  [intervention parent-iri session]
  (let [iri (generate-local-iri)
        params {"label" (get intervention "Intervention")}]
    (.run session "match (top:Assertion {iri: $parent})
merge (i:ActionabilityInterventionAssertion:Assertion:Entity {iri: $iri})
set i += $params
merge (i)-[:was_generated_by]->(top)
merge (i)-[:has_subject]->(top)"
          {"parent" parent-iri, "iri" iri, "params" params})
    (import-actionability-score (get intervention "Effectiveness") :efficacy iri session)
    (import-actionability-score (get intervention "Nature of Intervention") :safety iri session)))

(defn import-actionability-outcome
  "called by import-actionability-message, import outcome score to neo4j"
  [outcome parent-iri session]
  ;;(println "importing outcome " (get outcome "Outcome"))
  (let [iri (generate-local-iri)
        params {"label" (get outcome "Outcome")}]
    (.run session
          "match (top:Assertion {iri: $parent})
 merge (outcome:ActionabilityOutcomeAssertion:Assertion:Entity {iri: $iri})
 set outcome += $params
 merge (outcome)-[:was_generated_by]->(top)
 merge (outcome)-[:has_subject]->(top)"
          {"parent" parent-iri, "iri" iri, "params" params})
    (import-actionability-score (get outcome "Severity") :severity iri session)
    (import-actionability-score (get outcome "Likelihood") :likelihood iri session)
    (doseq [i (get outcome "Interventions")]
      (import-actionability-intervention i iri session))))

(defn import-actionability-message
  "Import message from actionability into neo4j"
  [message session] 
  (println "importing actionability message for " (get message "title"))
  (let [iri (get message "iri")
        params {"version" (get message "curationVersion")
                  "label" (get message "title")
                "date" (get message "dateISO8601")
                "report" (get message "scoreDetails")}
        genes (map #(get % "curie") (get message "genes"))
        conditions (map #(get % "iri") (get message "conditions"))
        predicate (:top act-iris)
        action (get message "statusFlag")
        affiliation-id (str agents-root (get-in message ["affiliation" "id"]))
        affiliation-name (get-in message ["affiliation" "name"])]
    (println "action: " action " iri: " iri)
    (when (and (= "Released" action) (re-matches #"^https://actionability\.clinicalgenome\.org/ac.*" iri))
      ;; (println "genes")
      ;; (pprint genes)
      ;; (println "conditions")
      ;; (pprint conditions)
      (.run session "merge (a:ActionabilityAssertion:Assertion:Entity {iri: $iri})
 with a
 match (g:Gene) where g.hgnc_id in $genes
 optional match (a)<-[:was_generated_by*1..5]-(n)
 detach delete n
 set a += $params 
 merge (a)-[:has_subject]->(g)
 merge (ag:Agent:Entity {iri: $affiliation_id})
 set ag.label = $affiliation_name
 merge (a)-[:wasAttributedto]->(ag)"
            {"iri" iri, "params" params, "genes" genes, "conditions" conditions, "affiliation_id" affiliation-id, "affiliation_name", affiliation-name})
      (.run session "match
 (r:RDFClass)-[:equivalentClass]-(c:DiseaseConcept), 
 (a {iri: $iri})
  where r.iri in $conditions
  merge (a)-[:has_object]->(c)" {"iri" iri, "conditions" conditions})
      (.run session "match
 (c:DiseaseConcept), 
 (a {iri: $iri})
  where c.iri in $conditions
  merge (a)-[:has_object]->(c)" {"iri" iri, "conditions" conditions})
      (doseq [outcome (get message "scores")]
        (import-actionability-outcome outcome iri session)))))

