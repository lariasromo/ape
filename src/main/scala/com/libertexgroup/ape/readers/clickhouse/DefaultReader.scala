package com.libertexgroup.ape.readers.clickhouse
import com.libertexgroup.ape.utils.ClickhouseJDBCUtils.query2ChunkMulti
import com.libertexgroup.configs.MultiClickhouseConfig
import zio.ZIO
import zio.stream.ZStream

import java.sql.ResultSet
import scala.reflect.ClassTag

protected[readers] class DefaultReader[E1, T: ClassTag] (sql:String)(implicit row2Object: ResultSet => T)
  extends com.libertexgroup.ape.readers.clickhouse.ClickhouseReader[MultiClickhouseConfig, E1, T] {

  override def apply: ZIO[MultiClickhouseConfig, Throwable, ZStream[E1, Throwable, T]] = for {
    chnk <- query2ChunkMulti(sql)
  } yield ZStream.fromChunk(chnk)

}
