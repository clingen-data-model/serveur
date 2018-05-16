(ns serveur.process-messages
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [cheshire.core :as json]
            [serveur.actionability :as actionability]
            [serveur.gene-validity :as gene-validity]
            [clojure.pprint :refer [pprint]]
            [serveur.neo4j :as neo]
            [serveur.kafka :as kafka]))


(defn process-message
  [message session]
  (let [type (get message "type")]
    (case type
      ;;"actionability" (actionability/import-actionability-message message session)
      "clinicalValidity" (gene-validity/import-gene-validity-message message session)
      (println "no handler for type: " type ))))

(defn process-local-messages
  "Read DX messages from local data directory"
  []
  (let [files (->> "data" io/file file-seq (filter #(.isFile %)))
        messages (map #(json/parse-stream (io/reader %)) files)]
    (neo/session
     [s]
     (doseq [m messages]
       (process-message m s)))))

(defn get-topics
  "retrieve the topics for Kafka to listen to from environment varaibles. expecting semicolon delimited list with no spaces, similar to path and other list-defined environment variables"
  []
  (string/split (System/getenv "SERVEUR_KAFKA_TOPICS") #";"))

(defn process-kafka-messages
  "Read messages from data exchange"
  []
  (with-open [consumer (kafka/consumer)]
    (.subscribe consumer (get-topics))
    (neo/session
     [neo-session]
     (while true)
     (println "polling")
     (let [records (.poll consumer 1000)]
       (doseq [r (seq records)]
         (process-message (-> r .value json/parse-string) neo-session))))))
