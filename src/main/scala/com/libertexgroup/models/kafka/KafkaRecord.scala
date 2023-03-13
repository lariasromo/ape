package com.libertexgroup.models.kafka

case class KafkaRecord(topic: String, message: String, timestamp: Long)
