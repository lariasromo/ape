package com.libertexgroup.ape.writers.kafka

import com.libertexgroup.ape.Writer
import com.libertexgroup.configs._
import com.sksamuel.avro4s.{Encoder, SchemaFor}
import org.apache.kafka.clients.producer.ProducerRecord
import zio.Tag

protected[writers] class Writers[Config <: KafkaConfig :Tag]() {
  def string[ET]: Writer[Config, ET, ProducerRecord[String, String], ProducerRecord[String, String]] =
    new DefaultWriter[ET, Config]

  def avro[ET, T: SchemaFor : Encoder]: Writer[Config, ET, ProducerRecord[String, T], ProducerRecord[String, T]] =
    new AvroWriter[ET, T, Config]
}
