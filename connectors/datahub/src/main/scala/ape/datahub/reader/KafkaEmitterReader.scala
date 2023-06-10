package ape.datahub.reader

import ape.datahub.configs.DatahubConfig
import ape.datahub.utils.DatahubUtils
import ape.reader.Reader
import com.sksamuel.avro4s.SchemaFor
import zio.ZIO
import zio.stream.ZStream

import scala.reflect.{ClassTag, classTag}

class KafkaEmitterReader[E, ZE, T: SchemaFor :ClassTag](r: Reader[E, ZE, T])
  extends Reader[E with DatahubConfig, ZE, T] {
  override def name: String = r.name

  override protected[this] def read: ZIO[E with DatahubConfig, Throwable, ZStream[ZE, Throwable, T]] = for {
    _ <- ZIO.logInfo("Dataset Name: " + classTag[T].runtimeClass.getSimpleName)
    _ <- DatahubUtils.emitKafka[T]
    s <- r.apply
  } yield s
}


object KafkaEmitterReader {
  def fromReader[E, ZE, T: SchemaFor :ClassTag](reader: Reader[E, ZE, T]): KafkaEmitterReader[E, ZE, T] =
    new KafkaEmitterReader[E, ZE, T](reader)

  def apply[E, ZE, T: SchemaFor :ClassTag] (reader: Reader[E, ZE, T]): KafkaEmitterReader[E, ZE, T] =
    fromReader[E, ZE, T](reader)
}