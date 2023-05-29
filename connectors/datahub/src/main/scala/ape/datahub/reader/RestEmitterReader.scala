package ape.datahub.reader

import ape.datahub.DatahubDataset
import ape.datahub.configs.DatahubConfig
import ape.datahub.utils.DatahubUtils
import ape.reader.Reader
import zio.Console.printLine
import zio.ZIO
import zio.stream.ZStream

import scala.reflect.ClassTag

class RestEmitterReader[E, ZE, T: DatahubDataset :ClassTag](r: Reader[E, ZE, T])
  extends Reader[E with DatahubConfig, ZE, T] {
  override def name: String = r.name

  override protected[this] def read: ZIO[E with DatahubConfig, Throwable, ZStream[ZE, Throwable, T]] = for {
    _ <- printLine("Dataset Name: " + implicitly[DatahubDataset[T]].datasetName)
    _ <- printLine("Dataset Description: " + implicitly[DatahubDataset[T]].datasetDescription)
    _ <- DatahubUtils.emitRest[T]
    s <- r.apply
  } yield s
}

object RestEmitterReader {
  def fromReader[E, ZE, T: DatahubDataset :ClassTag](reader: Reader[E, ZE, T]): RestEmitterReader[E, ZE, T] =
    new RestEmitterReader[E, ZE, T](reader)

  def apply[E, ZE, T: DatahubDataset :ClassTag] (reader: Reader[E, ZE, T]): RestEmitterReader[E, ZE, T] =
    fromReader[E, ZE, T](reader)
}