(defproject serveur "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :repl-options {:init-ns serveur.core}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.apache.kafka/kafka-clients "0.11.0.1"]
                 [cheshire "5.7.0"]
                 [org.neo4j.driver/neo4j-java-driver "1.4.4"]])
