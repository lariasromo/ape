package ape.jdbc

import ape.jdbc.configs.JDBCConfig
import ape.jdbc.readers.DefaultReader
import ape.reader.Reader
import ape.utils.Utils.:=
import zio.Tag

import java.sql.ResultSet
import scala.reflect.ClassTag

// Readers
protected [jdbc] class Readers[Config <: JDBCConfig :Tag]()(implicit d1: Config := JDBCConfig) {
  def default[T: ClassTag](sql: String)(implicit r2o: ResultSet => T, d1: Config := JDBCConfig): Reader[Config, Any, T] =
    new DefaultReader[Any, T, Config](sql)
}

object Readers {
  def readers[Config <: JDBCConfig :Tag](implicit d1: Config := JDBCConfig) = new Readers[Config]()
}