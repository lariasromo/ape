package com.libertexgroup.ape.readers.clickhouse

import com.libertexgroup.ape
import com.libertexgroup.ape.Reader
import com.libertexgroup.configs.MultiClickhouseConfig

import java.sql.ResultSet
import scala.reflect.ClassTag

// Readers
protected [readers] class Readers[Config <: MultiClickhouseConfig]() {
  def default[ET, T: ClassTag](sql: String)(implicit r2o: ResultSet => T):
    Reader[Config, ET, T] = new ape.readers.clickhouse.DefaultReader[ET, T, Config](sql)
}
