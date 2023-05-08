package com.libertexgroup.readers.jdbc

import com.libertexgroup.ape.reader.Reader
import com.libertexgroup.configs.JDBCConfig

import java.sql.ResultSet
import scala.reflect.ClassTag

trait JDBCReaders[Config <: JDBCConfig] {
  def default[ZE, T: ClassTag](sql: String)(implicit r2o: ResultSet => T): Reader[Config, ZE, T]
}