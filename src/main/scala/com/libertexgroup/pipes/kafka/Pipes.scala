package com.libertexgroup.pipes.kafka

import com.libertexgroup.ape.pipe.Pipe
import com.libertexgroup.configs._
import com.sksamuel.avro4s.{Encoder, SchemaFor}
import org.apache.kafka.clients.producer.ProducerRecord
import zio.Tag

import scala.reflect.ClassTag

protected[pipes] class Pipes[Config <: KafkaConfig :Tag]() {
  def string[ET]: Pipe[Config, ET, ProducerRecord[String, String], ProducerRecord[String, String]] =
    new DefaultPipe[ET, Config]

  def avro[ET, T: SchemaFor : Encoder]: Pipe[Config, ET, ProducerRecord[String, T], ProducerRecord[String, T]] =
    new AvroPipe[ET, T, Config]


  def stringEncode[ET, T: ClassTag]()(implicit enc: T => String):
  Pipe[Config, ET, ProducerRecord[String, T], ProducerRecord[String, T]] =
    new EncodePipe[ET, Config, T]()

  def stringEncodeCirce[ET, T: ClassTag : io.circe.Encoder]:
  Pipe[Config, ET, ProducerRecord[String, T], ProducerRecord[String, T]] = new EncodeCircePipe[ET, Config, T]
}
