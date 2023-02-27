//package com.libertexgroup.ape.pipelines
//
//import com.libertexgroup.ape
//import com.libertexgroup.configs.{ClickhouseConfig, JDBCConfig, KafkaConfig}
//import com.sksamuel.avro4s.{Decoder, Encoder, SchemaFor}
//import org.apache.kafka.clients.consumer.ConsumerRecord
//import sttp.ws.WebSocket
//import zio.kafka.consumer.Consumer
//import zio.{Duration, Task}
//
//import java.sql.ResultSet
//import scala.reflect.ClassTag
//
//class PipelineBuilder[E1, E2, E3, T1: ClassTag, T2: ClassTag]
//{
//  var reader: ape.readers.Reader[E1, E2, T1]
//  var transformer: ape.transformers.Transformer[E2, T1, T2]
//  var writer: ape.writers.Writer[E2, E3, T2]
//
//  // Readers
//  def withReader(r: ape.readers.Reader[E1, E2, T1]): PipelineBuilder[E1, E2, E3, T1, T2] = {
//    reader = r
//    this
//  }
//
//  def withDefaultClickhouseReader(sql:String)
//                                 (implicit row2Object: ResultSet => T1): PipelineBuilder[ClickhouseConfig, E2, E3, T1, T2] =
//  {
//    val r: ape.readers.Reader[ClickhouseConfig, E2, T1] = new ape.readers.clickhouse.DefaultReader(sql)
//    copy(reader = r)
//  }
//
//  def withDefaultJDBCReader(sql:String)
//                                 (implicit row2Object: ResultSet => T1): PipelineBuilder[JDBCConfig, E2, E3, T1, T2] =
//  {
//    val r: ape.readers.Reader[JDBCConfig, E2, T1] = new ape.readers.jdbc.DefaultReader(sql)
//    copy(reader = r)
//  }
//
//  def withDefaultKafkaReader: PipelineBuilder[KafkaConfig, Consumer, E3, ConsumerRecord[String, Array[Byte]], T2] = {
//  {
//    val r: ape.readers.Reader[KafkaConfig, Consumer, ConsumerRecord[String, Array[Byte]]] =
//      new ape.readers.kafka.DefaultReader()
//    copy(reader = r)
//  }
//}
//def withS3ParquetReader: PipelineBuilder[E1, E2, E3, T1, T2] = {
//    copy(reader = new ape.readers.s3.ParquetReader())
//}
//def withS3AvroReader[T >:Null :SchemaFor :Decoder :Encoder]: PipelineBuilder[E1, E2, E3, T1, T2] = {
//    copy(reader = new ape.readers.s3.AvroReader[T]())
//}
//def withS3TextReader: PipelineBuilder[E1, E2, E3, T1, T2] = {
//    copy(reader = new ape.readers.s3.TextReader())
//}
//def withS3TypedParquetReader[T >:Null :SchemaFor :Decoder :Encoder]: PipelineBuilder[E1, E2, E3, T1, T2] = {
//    copy(reader = new ape.readers.s3.TypedParquetReader[T]())
//}
//def withWebsocketReader(ws: WebSocket[Task]): PipelineBuilder[E1, E2, E3, T1, T2] = {
//    copy(reader = new ape.readers.websocket.DefaultReader(ws))
//}
//
  // Transformers
//  def withTransformer(transformer: ape.transformers.Transformer[E2, T1, T2]): PipelineBuilder[E1, E2, E3, T1, T2] = {
//    copy(transformer = transformer)
//
//  }
//
//  def withTransformer()(implicit algebra: T1 => T2): PipelineBuilder[E1, E2, E3, T1, T2] = {
//    copy(transformer = new ape.transformers.DefaultTransformer())
//
//  }
//
//  def withNoOpTransformer(): PipelineBuilder[E1, E2, E3, T1, T2] = {
//    copy(transformer = new ape.transformers.NoOpTransformer())
//
//  }
//
// // Writers
//  def withWriter(writer: ape.writers.Writer[E2, E3, T2]): PipelineBuilder[E1, E2, E3, T1, T2] = copy(writer = writer)
//
//  def withClickhouseWriter: PipelineBuilder[E1, E2, E3, T1, T2] = {
//    copy(writer = new ape.writers.clickhouse.DefaultWriter[E3])
//
//  }
//
//  def withJDBCWriter: PipelineBuilder[E1, E2, E3, T1, T2] = {
//    copy(writer = new ape.writers.jdbc.DefaultWriter[E3])
//
//  }
//
//  def withKafkaWriter: PipelineBuilder[E1, E2, E3, T1, T2] = {
//    copy(writer = new ape.writers.kafka.DefaultWriter[E3])
//
//  }
//
//  def withKafkaAvroWriter[T:SchemaFor :Encoder]: PipelineBuilder[E1, E2, E3, T1, T2] = {
//    copy(writer = new ape.writers.kafka.AvroWriter[E3, T])
//
//  }
//
//  def withS3BytesWriter[T >:Null :SchemaFor :Decoder :Encoder]: PipelineBuilder[E1, E2, E3, T1, T2] = {
//    copy(writer = new ape.writers.s3.BytesWriter[E3, T])
//
//  }
//
//  def withS3ParquetWriter[T >:Null: SchemaFor :Encoder :Decoder](chunkSize: Int,
//                                                                 duration: Duration): PipelineBuilder[E1, E2, E3, T1, T2] =
//    copy(writer = new ape.writers.s3.ParquetWriter[E3, T](chunkSize, duration))
//
//  def withS3TextWriter: PipelineBuilder[E1, E2, E3, T1, T2] = {
//    copy(writer = new ape.writers.s3.TextWriter[E3])
//}
//
//  def withConsoleWriter: PipelineBuilder[E1, E2, E3, T1, T2] = {
//    copy(writer = new ape.writers.ConsoleWriter)
//}
//def build: Pipeline[E1, E2, T1, T2, E3] = {
//    if (reader == null) throw new Exception("Failed to construct pipeline, Reader is null")
//    if (transformer == null) throw new Exception("Failed to construct pipeline, Transformer is null")
//    if (writer == null) throw new Exception("Failed to construct pipeline, Writer is null")
//    new Pipeline[E1, E2, T1, T2, E3](reader, transformer, writer)
//  }
//}
