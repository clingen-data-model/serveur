(ns serveur.gene-validity
  (require [serveur.neo4j :as neo]
           [cheshire.core :as json]))

(def significance-map {"Conflicting Evidence Reported" "http://datamodel.clinicalgenome.org/terms/CG_000068"
                       "Definitive" "http://datamodel.clinicalgenome.org/terms/CG_000063"
                       "Disputed" "http://datamodel.clinicalgenome.org/terms/CG_000084"
                       "Limited" "http://datamodel.clinicalgenome.org/terms/CG_000066"
                       "Moderate" "http://datamodel.clinicalgenome.org/terms/CG_000065"
                       "No Reported Evidence" "http://datamodel.clinicalgenome.org/terms/CG_000067" 
                       "Refuted" "http://datamodel.clinicalgenome.org/terms/CG_000085"
                       "Strong" "http://datamodel.clinicalgenome.org/terms/CG_000064"})

(defn conditions
  "Retrieve list of curies for condition"
  [message]
  (let [c-list (get message "conditions")]
    (map #(str "http://purl.obolibrary.org/obo/MONDO_"
               (re-find #"[0-9]+" (get % "curie"))) c-list)))

(defn genes
  "Retrieve list of curies for gene"
  [message]
  (let [g-list (get message "genes")]
    (map #(get % "curie") g-list)))

(defn serialize-score
  [message]
  (json/generate-string (get message "scoreJson")))

(defn unpublish
  "Unpublish existing curation"
  [message session]
  (println "unpublishing iri"))

(defn get-significance
  "Given the message, return the appropriate clinical significance value"
  [message]
  (get significance-map (get-in message ["scoreJson" "summary" "FinalClassification"])))

(defn get-mode-of-inheritance
  "Retrieve the HPO IRI for the mode of inheritance of the condition"
  [message]
  (let  [s (get-in message ["scoreJson" "ModeOfInheritance"])
         code (last (re-find #"\(HP:(\d+)\)" s))]
    (str "http://purl.obolibrary.org/obo/HP_" code)))

;; TODO add test for sequencing date-wise
(defn replace-previous-gci-curation
  "Test to see if there is a current, active curation from the GCI in the database
   if there is, mark it as invalidated by the current one"
  [iri gci-id session]
  (.run session "match (current:GeneDiseaseAssertion {iri: $iri})
match (previous:GeneDiseaseAssertion {gci_id: $gci_id}) where not previous.iri = $iri and not (previous)-[:wasInvalidatedBy]->()
merge (previous)-[:wasInvalidatedBy]->(current)"
        {"iri" iri, "gci_id" gci-id,}))


;; TODO take into account mode of inheritance
;; TODO retrospectively populate mode of inheritance
(defn replace-previous-gene-express-curation
  "Test to see if there is a previous, matching curation from the Gene Curation Express
  interface, if so, mark it as invalidated by the current curation. Use ontological
  equivalence to identify identical assertions. Note that if the target gene and also
  the target disease match it will be considered an equivalent assertion"
  [iri session]
  (.run session "match (current:GeneDiseaseAssertion {iri: $iri})
 match (previous:GeneDiseaseAssertion)
 where previous.gci_id is null
 and (previous)-[:has_subject]->(:Gene)<-[:has_subject]-(current)
 and 
 ((previous)-[:has_object]->(:RDFClass)<-[:has_object]-(current)
 or (previous)-[:has_object]->(:RDFClass)-[:equivalentTo]-(:RDFClass)<-[:has_object]-(current))
 merge (previous)-[:wasInvalidatedBy]->(current)" {"iri" iri}))

;; TODO consider restructuring around a single transaction instead of multiple
;; single queries
(defn publish
  "Publishing new curation"
  [message session]
  (let [conditions (conditions message)
        genes (genes message)
        gci-id (get message "iri")
        attr-subset (select-keys message ["sopVersion" "curationVersion"
                                          "jsonMessageVersion" "title"])
        date (get-in message ["scoreJson" "summary" "FinalClassificationDate"])
        iri (str (get message "iri") "--" date) ;; ID in DB is combination of gciid + date
        moi (get-mode-of-inheritance message)
        curation-attributes (-> attr-subset (assoc "date" date)
                                (assoc "score_gci" (serialize-score message))
                                (assoc "gci_id" gci-id))
        significance (get-significance message)
        pub-status (get message "statusPublishFlag")]
    (println "publishing" (get message "iri"))
    (println conditions)
    (println genes)
    (println date)
    ;; TODO start here
    (.run session "merge (a:GeneDiseaseAssertion:Assertion:Entity {iri: $iri})
    with a
    match (g:Gene) where g.hgnc_id in $genes
    match (c:RDFClass) where c.iri in $conditions
    match (s:Interpretation {iri: $significance})
    match (moi:RDFClass {iri: $moi})
    set a += $attributes
    merge (a)-[:has_subject]->(g)
    merge (a)-[:has_object]->(c)
merge (a)-[:has_predicate]->(s)
merge (a)-[:has_object]->(moi)"
          {"genes" genes, "conditions" conditions, "attributes" curation-attributes,
           "significance" significance, "iri" iri, "moi" moi})
    (replace-previous-gci-curation iri gci-id session)
    (replace-previous-gene-express-curation iri session)))

(defn import-gene-validity-message
  "Import message from gene validity into neo4j"
  [message session]
  (println "Importing gene validity message")
  (publish message session)
  ;; (let [status (get message "statusPublishFlag")]
  ;;   (case status
  ;;     "Unpublish" (unpublish message session)
  ;;     "Publish" (publish message session)
  ;;     (println "Error: unknown status")))
  )