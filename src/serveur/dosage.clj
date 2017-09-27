(ns serveur.dosage
  (:require [serveur.kafka :as kafka]
            [serveur.neo4j :as neo]
            [cheshire.core :as json]))

(defn import-dosage-record
  "Import a single gene-dosage record into neo4j"
  [record session]
  ;; (spit (str "./log/" (re-find #"[\w-]+$" (.key record))) record)
  (let [interp (-> record .value json/parse-string)]
    (println "importing: " (interp "iri"))
    (.run session "merge "))) ; TODO start here, do neo import statement

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
