package com.libertexgroup.ape.writers.kafka

import com.libertexgroup.ape.Writer
import com.libertexgroup.configs._
import com.sksamuel.avro4s.{Encoder, SchemaFor}
import org.apache.kafka.clients.producer.ProducerRecord
import zio.Tag

import scala.reflect.ClassTag

protected[writers] class Writers[Config <: KafkaConfig :Tag]() {
  def string[ET]: Writer[Config, ET, ProducerRecord[String, String], ProducerRecord[String, String]] =
    new DefaultWriter[ET, Config]

  def avro[ET, T: SchemaFor : Encoder]: Writer[Config, ET, ProducerRecord[String, T], ProducerRecord[String, T]] =
    new AvroWriter[ET, T, Config]


  def stringEncode[ET, T: ClassTag]()(implicit enc: T => String):
  Writer[Config, ET, ProducerRecord[String, T], ProducerRecord[String, T]] =
    new EncodeWriter[ET, Config, T]()

  def stringEncodeCirce[ET, T: ClassTag : io.circe.Encoder]:
  Writer[Config, ET, ProducerRecord[String, T], ProducerRecord[String, T]] = new EncodeCirceWriter[ET, Config, T]
}
