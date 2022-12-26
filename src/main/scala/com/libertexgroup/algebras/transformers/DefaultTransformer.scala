package com.libertexgroup.algebras.transformers
import org.apache.kafka.clients.consumer.ConsumerRecord
import zio.stream.ZStream

class DefaultTransformer[E] extends Transformer[E, ConsumerRecord[String, Array[Byte]], Array[Byte]] {
  override def apply(stream: ZStream[E, Throwable, ConsumerRecord[String, Array[Byte]]]): ZStream[E, Throwable, Array[Byte]] =
    stream.map { t => t.value() }
}
