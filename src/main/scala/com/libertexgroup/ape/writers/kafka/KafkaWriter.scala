package com.libertexgroup.ape.writers.kafka

import com.libertexgroup.ape.Writer
import org.apache.kafka.clients.producer.ProducerRecord

abstract class KafkaWriter[E, E1, T1, T2] extends Writer[E, E1, ProducerRecord[T1, T2], ProducerRecord[T1, T2]] {
  override val name = "KafkaWriter"
}
