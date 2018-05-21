(ns serveur.core
  (:require [serveur.kafka :as kafka]
            [cheshire.core :as json]
            [clojure.tools.logging :as log]
            [clojure.tools.cli :refer [parse-opts]]
            [clojure.java.io :as io]
            [serveur.neo4j :as neo]
            [cheshire.core :as json]
            [serveur.process-messages :as m])
  (:gen-class))

(def import-directory "data/import/")

(def cli-options
  [["-r" "--read-files"]])  ;; Read input from files in data/import dir

(defn -main
  "Start the kafka-polling loop. Keep it running."
  [& args]
  (let [opts (parse-opts args cli-options)]
    (if (-> opts :options :read-files)
      (do (println "reading curations from data/import")
          (m/process-local-messages))
      (do (log/info "Starting serveur update loop")
          (m/process-kafka-messages)))))
