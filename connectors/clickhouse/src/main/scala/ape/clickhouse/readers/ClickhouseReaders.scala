package ape.clickhouse.readers

import ape.reader.Reader

import java.sql.ResultSet
import scala.reflect.ClassTag

trait ClickhouseReaders[Config] {
  def default[T: ClassTag](sql: String)(implicit r2o: ResultSet => T): Reader[Config, Any, T]
}