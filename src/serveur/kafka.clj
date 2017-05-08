(ns serveur.kafka
  (:import java.util.Properties
           [org.apache.kafka.clients.consumer KafkaConsumer Consumer ConsumerRecord]))

(def client-properties
  {"bootstrap.servers" "tndeb:9093"
   "group.id" "test"
   "enable.auto.commit" "true"
   "auto.commit.interval.ms" "1000"
   "key.deserializer" "org.apache.kafka.common.serialization.StringDeserializer"
   "value.deserializer" "org.apache.kafka.common.serialization.StringDeserializer"
   "security.protocol" "SSL"
   "ssl.truststore.location" "ssl/client.truststore.jks"
   "ssl.truststore.password" "test1234"
   "ssl.keystore.location" "ssl/client.keystore.jks"
   "ssl.keystore.password" "test1234"
   "ssl.key.password" "test1234"})

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
