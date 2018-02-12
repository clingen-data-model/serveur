(ns serveur.core
  (:require [serveur.kafka :as kafka]
            [cheshire.core :as json]
            [serveur.dosage :as dosage]
            [clojure.tools.logging :as log]
            [clojure.tools.cli :refer [parse-opts]]
            [clojure.java.io :as io]
            [serveur.neo4j :as neo]
            [cheshire.core :as json])
  (:gen-class))

(def import-directory "data/import/")

(def cli-options
  [["-r" "--read-files"]] ;; Read input from files in data/import dir
  )

(defn import-from-directory
  [dir]
  (let [fs (-> dir io/file file-seq)]
    (neo/session
     [session]
     (doseq [f fs]
       (when (.isFile f)
         (let [record (-> f io/reader json/parse-stream)
               type (get record "type")]
           (cond
             (= "http://datamodel.clinicalgenome.org/terms/CG_000083" type)
             (dosage/import-dosage-record record session)
             (= "http://datamodel.clinicalgenome.org/terms/CG_000116" type)
             (println "region dosage record")
             :else (println "unknown type"))))))))

(defn -main
  "Start the kafka-polling loop. Keep it running."
  [& args]
  (let [opts (parse-opts args cli-options)]
    (if (-> opts :options :read-files)
      (do (println "reading curations from data/import")
          (import-from-directory import-directory))
      (do (log/info "Starting serveur update loop")
          (dosage/update-loop)))))
