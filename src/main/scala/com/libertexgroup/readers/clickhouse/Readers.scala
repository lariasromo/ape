package com.libertexgroup.readers.clickhouse

import com.libertexgroup.ape.reader.Reader
import com.libertexgroup.configs.MultiClickhouseConfig

import java.sql.ResultSet
import scala.reflect.ClassTag

class Readers[Config <: MultiClickhouseConfig] extends ClickhouseReaders[Config] {
  def default[ZE, T: ClassTag](sql: String)(implicit r2o: ResultSet => T): Reader[Config, ZE, T] =
    new DefaultReader[ZE, T, Config](sql)
}

