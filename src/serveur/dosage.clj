(ns serveur.dosage
  (:require [serveur.kafka :as kafka]
            [serveur.neo4j :as neo]
            [cheshire.core :as json]
            [clojure.tools.logging :as log]
            [clojure.pprint :refer [pprint]]))

(defn import-dosage-record
  "Import a single gene-dosage record into neo4j"
  [record session]
  ;; (spit (str "./log/" (re-find #"[\w-]+$" (.key record))) record)
  (println "importing: " (record "id"))
  (log/info "importing: " (with-out-str (pprint record)))
  (.run session "match (a:GeneDosageAssertion {iri: $id})-[r:has_predicate]->() delete r with a match (a)-[r:has_object]->() delete r" record)
  (.run session "match (g:Gene {iri: $gene}), (i:RDFClass {iri: $interpretation}) merge (a:GeneDosageAssertion:Assertion:Entity {iri: $id}) set a.date = $modified merge (a)-[:has_subject]->(g) merge (a)-[:has_predicate]->(i) with a match (d:RDFClass {iri: $phenotype})-[:equivalentTo]-(dc:DiseaseConcept) merge (a)-[:has_object]->(dc)" record))

(defn import-region-dosage-record
  "Import a single region-dosage record into neo4j"
  [record session])

(defn update-loop
  "Poll kafka for updates and import to knowledge base"
  []
  (with-open [consumer (kafka/consumer)]
    (.subscribe consumer ["gene_dosage"])
    (neo/session
     [neo-session]
     (while true
       (println "polling")
       (let [records (.poll consumer 1000)]
         (doseq [r (seq records)]
           (import-dosage-record (-> r .value json/parse-string) neo-session)))))))
