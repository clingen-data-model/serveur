(ns serveur.core
  (:require [serveur.kafka :as kafka]
            [cheshire.core :as json]
            [serveur.dosage :as dosage]
            [clojure.tools.logging :as log]
            [clojure.tools.cli :refer [parse-opts]])
  (:gen-class))

(def cli-options
  [["-r" "--read-files"]] ;; Read input from files in data/import dir
  )

(defn -main
  "Start the kafka-polling loop. Keep it running."
  [& args]
  (let [opts (parse-opts args cli-options)]
    (if (-> opts :options :read-files)
      (println "reading curations from data/import")
      (do (log/info "Starting serveur update loop")
          (dosage/update-loop)))))
