package com.libertexgroup.pipes.kafka

import com.libertexgroup.ape.pipe.Pipe
import com.libertexgroup.configs._
import com.libertexgroup.models.jdbc.JDBCModel
import com.libertexgroup.utils.Utils.:=
import com.sksamuel.avro4s.{Encoder, SchemaFor}
import org.apache.kafka.clients.producer.ProducerRecord
import zio.Tag

import scala.reflect.ClassTag

protected[pipes] class Pipes[Config <: KafkaConfig :Tag]() {
  class avro[ET] {
    def of[T: SchemaFor : Encoder]: Pipe[Config, ET, ProducerRecord[String, T], ProducerRecord[String, T]] = new AvroPipe[ET, T, Config]
  }
  def avro[ET](implicit d1: ET := Any) = new avro[ET]

  class string[ET] {
    def default: Pipe[Config, ET, ProducerRecord[String, String], ProducerRecord[String, String]] =
      new DefaultPipe[ET, Config]
    def encode[T: ClassTag]()(implicit enc: T => String):
      Pipe[Config, ET, ProducerRecord[String, T], ProducerRecord[String, T]] = new EncodePipe[ET, Config, T]()
    def encodeCirce[T: ClassTag : io.circe.Encoder]:
      Pipe[Config, ET, ProducerRecord[String, T], ProducerRecord[String, T]] = new EncodeCircePipe[ET, Config, T]
  }
  def string[ET](implicit d1: ET := Any) = new string[ET]
}
