package com.libertexgroup.readers.jdbc

import com.libertexgroup.ape.reader.Reader
import com.libertexgroup.configs._
import zio.Tag

import java.sql.ResultSet
import scala.reflect.ClassTag


// Readers
protected [readers] class Readers[Config <: JDBCConfig :Tag]() extends JDBCReaders[Config] {
  def default[ZE, T: ClassTag](sql: String)(implicit r2o: ResultSet => T): Reader[Config, ZE, T] =
    new DefaultReader[ZE, T, Config](sql)
}
