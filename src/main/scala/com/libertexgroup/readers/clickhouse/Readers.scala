package com.libertexgroup.readers.clickhouse

import com.libertexgroup.ape.reader.Reader
import com.libertexgroup.configs.MultiClickhouseConfig

import java.sql.ResultSet
import scala.reflect.ClassTag

class Readers[Config <: MultiClickhouseConfig] extends ClickhouseReaders[Config] {
  def default[T: ClassTag](sql: String)(implicit r2o: ResultSet => T): Reader[Config, Any, T] =
    new DefaultReader[Any, T, Config](sql)
}

