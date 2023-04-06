package com.libertexgroup.ape.readers.clickhouse
import com.libertexgroup.ape.utils.ClickhouseJDBCUtils.query2ChunkMulti
import com.libertexgroup.configs.MultiClickhouseConfig
import zio.ZIO
import zio.stream.ZStream

import java.sql.ResultSet
import scala.reflect.ClassTag

protected[clickhouse] class DefaultReader[E1, T: ClassTag, Config <: MultiClickhouseConfig](sql:String)
                                                                                        (implicit r: ResultSet => T)
  extends ClickhouseReader[Config, E1, T] {

  override def apply: ZIO[MultiClickhouseConfig, Throwable, ZStream[E1, Throwable, T]] =
    for {
      chnk <- query2ChunkMulti(sql)
    } yield ZStream.fromChunk(chnk)
}
