package com.libertexgroup.readers.kafka

import com.libertexgroup.ape.reader.Reader
import com.libertexgroup.configs._
import com.sksamuel.avro4s.{Decoder, Encoder, SchemaFor}
import org.apache.kafka.clients.consumer.ConsumerRecord
import zio.Tag

import scala.reflect.ClassTag

// Readers
protected [readers] class Readers[Config <: KafkaConfig :Tag]() {
  def default: Reader[Config, Any, ConsumerRecord[String, Array[Byte]]] =
    new DefaultReader()

  def avro[T >:Null :SchemaFor :Decoder :Encoder]: Reader[Config, Any, ConsumerRecord[String, Option[T]]] =
    new AvroReader[T, Config]()

  def string: Reader[Config, Any, ConsumerRecord[String, String]] =
    new StringReader()

  def jsonCirce[T >:Null :io.circe.Decoder : ClassTag]: Reader[Config, Any, ConsumerRecord[String, T]] =
    new JsonCirceReader[T, Config]()
}
