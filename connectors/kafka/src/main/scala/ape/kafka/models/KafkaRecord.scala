package ape.kafka.models

case class KafkaRecord(topic: String, message: String, timestamp: Long)
