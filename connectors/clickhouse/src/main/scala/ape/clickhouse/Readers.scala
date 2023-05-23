package ape.clickhouse

import ape.clickhouse.configs.MultiClickhouseConfig
import ape.clickhouse.readers.{ClickhouseReaders, DefaultReader}
import ape.reader.Reader
import ape.utils.Utils.:=
import zio.Tag

import java.sql.ResultSet
import scala.reflect.ClassTag

protected [clickhouse] class Readers[Config <: MultiClickhouseConfig] extends ClickhouseReaders[Config] {
  def default[T: ClassTag](sql: String)(implicit r2o: ResultSet => T): Reader[Config, Any, T] =
    new DefaultReader[Any, T, Config](sql)
}

object Readers {
  def readers[Config <: MultiClickhouseConfig :Tag](implicit d1: Config := MultiClickhouseConfig) = new Readers[Config]()
}