package com.libertexgroup.readers.jdbc

import com.libertexgroup.ape.reader.Reader
import com.libertexgroup.configs._
import com.libertexgroup.utils.Utils.:=
import zio.Tag

import java.sql.ResultSet
import scala.reflect.ClassTag


// Readers
protected [readers] class Readers[Config <: JDBCConfig :Tag]()(implicit d1: Config := JDBCConfig) {
  def default[T: ClassTag](sql: String)(implicit r2o: ResultSet => T, d1: Config := JDBCConfig): Reader[Config, Any, T] =
    new DefaultReader[Any, T, Config](sql)
}
