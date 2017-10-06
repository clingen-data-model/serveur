(ns serveur.core
  (:require [serveur.kafka :as kafka]
            [cheshire.core :as json]
            [serveur.dosage :as dosage]
            [clojure.tools.logging :as log])
  (:gen-class))

(defn -main
  "Start the kafka-polling loop. Keep it running."
  [& args]
  (log/info "Starting serveur update loop")
  (dosage/update-loop))
