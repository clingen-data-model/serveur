(ns serveur.kafka
  (:import java.util.Properties
           [org.apache.kafka.clients.consumer KafkaConsumer Consumer ConsumerRecord]))

(defn truststore-path
  []
  (if (= (System/getenv "SERVEUR_MODE") "development")
    "keys/dev.serveur.keystore.jks"
    "keys/serveur.keystore.jks"))

(defn group-id
  []
  (if (= (System/getenv "SERVEUR_MODE") "development")
    "serveur_dev"
    ;; "User:CN=dev.serveur.clinicalgenome.org,OU=Unknown,O=Unknown,L=New York,ST=New York,C=US"
    "serveur"))

(def client-properties
  {"bootstrap.servers" (System/getenv "DATA_EXCHANGE_HOST")
   "group.id" (group-id)
   "enable.auto.commit" "true"
   "auto.commit.interval.ms" "1000"
   "key.deserializer" "org.apache.kafka.common.serialization.StringDeserializer"
   "value.deserializer" "org.apache.kafka.common.serialization.StringDeserializer"
   "security.protocol" "SSL"
   "ssl.truststore.location" "keys/serveur.truststore.jks"
   "ssl.truststore.password" (System/getenv "SERVEUR_KEY_PASS")
   "ssl.keystore.location" (truststore-path)
   "ssl.keystore.password" (System/getenv "SERVEUR_KEY_PASS")
   "ssl.key.password" (System/getenv "SERVEUR_KEY_PASS")})

;; Java Properties object defining configuration of Kafka client
(defn client-configuration 
  "Create client "
  []
  (let [props (new Properties)]
    (doseq [p client-properties]
      (.put props (p 0) (p 1)))
    props))

(defn consumer
  []
  (let [props (client-configuration)]
    (new KafkaConsumer props)))
