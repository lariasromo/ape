package ape.kafka

import ape.kafka.configs.KafkaConfig
import ape.kafka.readers._
import ape.reader.Reader
import ape.utils.Utils.:=
import com.sksamuel.avro4s.{Decoder, Encoder, SchemaFor}
import org.apache.kafka.clients.consumer.ConsumerRecord
import zio.Tag

import scala.reflect.ClassTag


// Readers
protected [kafka] class Readers[Config <: KafkaConfig :Tag]() extends KafkaReaders[Config] {
  def default: Reader[Config, Any, ConsumerRecord[String, Array[Byte]]] =
    new DefaultReader()

  def avro[T >:Null :SchemaFor :Decoder :Encoder]: Reader[Config, Any, ConsumerRecord[String, Option[T]]] =
    new AvroReader[T, Config]()

  def string: Reader[Config, Any, ConsumerRecord[String, String]] =
    new StringReader()

  def jsonCirce[T >:Null :io.circe.Decoder : ClassTag]: Reader[Config, Any, ConsumerRecord[String, T]] =
    new JsonCirceReader[T, Config]()
}

object Readers {
  def readers[Config <: KafkaConfig :Tag](implicit d1: Config := KafkaConfig) = new Readers[Config]()
}