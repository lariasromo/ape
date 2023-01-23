package com.libertexgroup.algebras.transformers

import org.apache.avro.generic.GenericRecord
import zio.stream.ZStream

class GenericRecord2StringTransformer[F] extends Transformer[F, GenericRecord, String] {
  override def apply(stream: ZStream[F, Throwable, GenericRecord]): ZStream[F, Throwable, String] =
    stream.map { t => t.toString }
}
