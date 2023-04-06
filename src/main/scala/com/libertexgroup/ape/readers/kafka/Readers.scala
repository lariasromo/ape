package com.libertexgroup.ape.readers.kafka

import com.libertexgroup.ape
import com.libertexgroup.ape.Reader
import com.libertexgroup.configs._
import com.sksamuel.avro4s.{Decoder, Encoder, SchemaFor}
import org.apache.kafka.clients.consumer.ConsumerRecord
import zio.kafka.consumer.Consumer

import scala.reflect.ClassTag

// Readers
protected [readers] class Readers[Config <: KafkaConfig]() {
  def default: Reader[Config, Consumer, ConsumerRecord[String, Array[Byte]]] =
    new ape.readers.kafka.DefaultReader()

  def avro[T >:Null :SchemaFor :Decoder :Encoder]: Reader[Config, Consumer, ConsumerRecord[String, Option[T]]] =
    new ape.readers.kafka.AvroReader[T, Config]()

  def string: Reader[Config, Consumer, ConsumerRecord[String, String]] =
    new ape.readers.kafka.StringReader()

  def jsonCirce[T >:Null :io.circe.Decoder : ClassTag]: Reader[Config, Consumer, ConsumerRecord[String, T]] =
    new ape.readers.kafka.JsonCirceReader[T, Config]()
}