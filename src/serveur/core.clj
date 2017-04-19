(ns serveur.core
  (:require [neoclj.core :as neo]
            [serveur.kafka :as kafka]
            [cheshire.core :as json]))


(defn import-dosage
  "Import a gene dosage interpretation into Neo"
  [interp]
  (neo/session
   [s]
   (let [entrez_id (re-find #"\d+$" (interp "entrez_gene"))
         uuid (str (java.util.UUID/randomUUID))]
     ;; for some reason need to call toString, even though input is a string
     ;; should investigate, but workaround works for now
     (.run s "MATCH (g:Gene {entrez_id: toString($entrez_id)})
 MERGE (a:GeneDosageAssertion:Assertion:Entity {iri: $id})
 ON CREATE SET a.uuid = $uuid
 MERGE (a)-[:has_subject]->(g)"
           {"entrez_id" entrez_id
            "id" (interp "id")
            "uuid" uuid}))))

(defn poll-kafka
  "poll remote kafka server for dosage assertions"
  [topics callback]
  (with-open [c (kafka/consumer)]
    (.subscribe c topics)
    (let [results (.poll c 100)]
      (doseq [r results]
        (-> r .value json/parse-string callback)))))

(defn foo
  "I don't do a whole lot."
  [x]
  (println x "Hello, World!"))
