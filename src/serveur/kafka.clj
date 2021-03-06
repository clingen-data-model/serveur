(ns serveur.kafka
  (:import java.util.Properties
           [org.apache.kafka.clients.consumer KafkaConsumer Consumer ConsumerRecord]))

(def client-properties
  {"bootstrap.servers" (System/getenv "DATA_EXCHANGE_HOST")
   "group.id" (System/getenv "SERVEUR_GROUP")
   "enable.auto.commit" "true"
   "auto.commit.interval.ms" "1000"
   "key.deserializer" "org.apache.kafka.common.serialization.StringDeserializer"
   "value.deserializer" "org.apache.kafka.common.serialization.StringDeserializer"
   "security.protocol" "SSL"
   "ssl.truststore.location" "keys/serveur.truststore.jks"
   "ssl.truststore.password" (System/getenv "SERVEUR_TRUST_PASS")
   "ssl.keystore.location" (System/getenv "SERVEUR_KEYSTORE")
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
