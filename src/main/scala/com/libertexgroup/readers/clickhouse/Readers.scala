package com.libertexgroup.readers.clickhouse

import com.libertexgroup.ape.reader.Reader
import com.libertexgroup.configs.MultiClickhouseConfig

import java.sql.ResultSet
import scala.reflect.ClassTag

// Readers
protected [readers] class Readers[Config <: MultiClickhouseConfig]() {
  def default[ET, T: ClassTag](sql: String)(implicit r2o: ResultSet => T):
    Reader[Config, ET, T] = new DefaultReader[ET, T, Config](sql)
}
