package ape.jdbc.readers

import ape.jdbc.configs.JDBCConfig
import ape.reader.Reader

import java.sql.ResultSet
import scala.reflect.ClassTag

trait JDBCReaders[Config <: JDBCConfig] {
  def default[T: ClassTag](sql: String)(implicit r2o: ResultSet => T): Reader[Config, Any, T]
}