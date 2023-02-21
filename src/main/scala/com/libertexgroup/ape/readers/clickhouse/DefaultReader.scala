package com.libertexgroup.ape.readers.clickhouse
import com.libertexgroup.ape.utils.ClickhouseJDBCUtils.query2Chunk
import com.libertexgroup.configs.ClickhouseConfig
import zio.ZIO
import zio.stream.ZStream

import java.sql.ResultSet
import scala.reflect.ClassTag

class DefaultReader[E1, T: ClassTag](sql:String)
                          (implicit row2Object: ResultSet => T) extends ClickhouseReader[ClickhouseConfig, E1, T] {

  override def apply: ZIO[ClickhouseConfig, Throwable, ZStream[E1, Throwable, T]] = for {
    chnk <- query2Chunk(sql)
  } yield ZStream.fromChunk(chnk)

}
