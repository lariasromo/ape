package ape.kafka.readers

import ape.kafka.configs.KafkaConfig
import ape.reader.Reader
import com.sksamuel.avro4s.{Decoder, Encoder, SchemaFor}
import org.apache.kafka.clients.consumer.ConsumerRecord

import scala.reflect.ClassTag

trait KafkaReaders[Config <: KafkaConfig] {
  def default: Reader[Config, Any, ConsumerRecord[String, Array[Byte]]]
  def avro[T >:Null :SchemaFor :Decoder :Encoder]: Reader[Config, Any, ConsumerRecord[String, Option[T]]]
  def string: Reader[Config, Any, ConsumerRecord[String, String]]
  def jsonCirce[T >:Null :io.circe.Decoder : ClassTag]: Reader[Config, Any, ConsumerRecord[String, T]]
}