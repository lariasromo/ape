package com.libertexgroup.algebras.transformers.clickhouse

import com.libertexgroup.algebras.transformers.Transformer
import com.libertexgroup.models.ClickhouseModel
import com.libertexgroup.models.clickhouse.DefaultModel
import org.apache.kafka.clients.consumer.ConsumerRecord
import zio.stream.ZStream

class DefaultClickhouseTransformer[E, I] extends Transformer[E, I] {
  type OutputType = ClickhouseModel
  override def apply(stream: ZStream[E, Throwable, I]): ZStream[E, Throwable, ClickhouseModel] = {
    stream.map {
      case t: ConsumerRecord[String, Array[Byte]] => DefaultModel(t.value())
      case t => throw new Exception(s"Transforming from ${t.getClass} is not supported")
    }
  }
}
