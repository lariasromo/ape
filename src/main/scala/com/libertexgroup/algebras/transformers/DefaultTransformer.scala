package com.libertexgroup.algebras.transformers
import org.apache.kafka.clients.consumer.ConsumerRecord
import zio.stream.ZStream

class DefaultTransformer[E, I] extends Transformer[E, I] {
  type OutputType = Array[Byte]
  override def apply(stream: ZStream[E, Throwable, I]): ZStream[E, Throwable, OutputType] = {
    stream.map {
      case t: ConsumerRecord[String, Array[Byte]] => t.value()
      case t => throw new Exception(s"Transforming from ${t.getClass} is not supported")
    }
  }
}
