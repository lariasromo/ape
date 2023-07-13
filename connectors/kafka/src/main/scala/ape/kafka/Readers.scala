package ape.kafka

import ape.kafka.configs.KafkaConfig
import ape.kafka.readers._
import ape.reader.Reader
import ape.utils.Utils.:=
import com.sksamuel.avro4s.{Decoder, Encoder, SchemaFor}
import org.apache.kafka.clients.consumer.ConsumerRecord
import zio.{Chunk, Tag}

import scala.reflect.ClassTag


// Readers
protected [kafka] class ReadersFlattened[Config <: KafkaConfig :Tag]() {
  def default: Reader[Config, Any, ConsumerRecord[String, Array[Byte]]] =
    new DefaultReaderFlattened[Config]()

  def avro[T >:Null :SchemaFor :Decoder :Encoder]: Reader[Config, Any, ConsumerRecord[String, Option[T]]] =
    new AvroReaderFlattened[T, Config]()

  def string: Reader[Config, Any, ConsumerRecord[String, String]] =
    new StringReaderFlattened[Config]()

  def jsonCirce[T >:Null :io.circe.Decoder : ClassTag]: Reader[Config, Any, ConsumerRecord[String, T]] =
    new JsonCirceReaderFlattened[T, Config]()
}

// Readers
protected [kafka] class Readers[Config <: KafkaConfig :Tag]() {
  def default: Reader[Config, Any, Chunk[ConsumerRecord[String, Array[Byte]]]] =
    new DefaultReader[Config]()

  def avro[T >:Null :SchemaFor :Decoder :Encoder]: Reader[Config, Any, Chunk[ConsumerRecord[String, Option[T]]]] =
    new AvroReader[T, Config]()

  def string: Reader[Config, Any, Chunk[ConsumerRecord[String, String]]] =
    new StringReader[Config]()

  def jsonCirce[T >:Null :io.circe.Decoder : ClassTag]: Reader[Config, Any, Chunk[ConsumerRecord[String, T]]] =
    new JsonCirceReader[T, Config]()
}

object Readers {
  def readers[Config <: KafkaConfig :Tag](implicit d1: Config := KafkaConfig) = new Readers[Config]()
  def readersFlattened[Config <: KafkaConfig :Tag](implicit d1: Config := KafkaConfig) = new ReadersFlattened[Config]()
}