package com.libertexgroup.algebras.transformers.clickhouse

import com.libertexgroup.algebras.transformers.Transformer
import com.libertexgroup.models.ClickhouseModel
import com.libertexgroup.models.clickhouse.DefaultModel
import org.apache.kafka.clients.consumer.ConsumerRecord
import zio.stream.ZStream

class DefaultClickhouseTransformer[E] extends Transformer[E, ConsumerRecord[String, Array[Byte]], ClickhouseModel] {
  override def apply(stream: ZStream[E, Throwable, ConsumerRecord[String, Array[Byte]]]):
    ZStream[E, Throwable, DefaultModel] = {
    stream.map { t =>  DefaultModel(t.value()) }
  }
}
