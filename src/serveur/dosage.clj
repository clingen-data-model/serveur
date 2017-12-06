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
  (let [interp (-> record .value json/parse-string)]
    (pprint interp)
    (println "importing: " (interp "iri"))
    (log/info "importing: " (with-out-str (pprint interp)))
    ;; TODO clear relations before updating an assertion
    (.run session "match (g:Gene {iri: $gene}), (i:RDFClass {iri: $interpretation}) merge (a:GeneDosageAssertion:Assertion:Entity {iri: $iri}) set a.date = $modified merge (a)-[:has_subject]->(g) merge (a)-[:has_predicate]->(i) with a match (d:RDFClass {iri: $phenotype}) merge (a)-[:has_object]->(d)" interp)))

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
           (import-dosage-record r neo-session)))))))
