package ape.clickhouse.readers

import ape.clickhouse.configs.MultiClickhouseConfig
import ape.clickhouse.utils.ClickhouseJDBCUtils.query2ChunkMulti
import zio.ZIO
import zio.stream.ZStream

import java.sql.ResultSet
import scala.reflect.ClassTag

protected[clickhouse] class DefaultReader[E1, T: ClassTag, Config <: MultiClickhouseConfig](sql:String)
                                                                                        (implicit r: ResultSet => T)
  extends ClickhouseReader[Config, E1, T] {

  override protected[this] def read: ZIO[Config, Throwable, ZStream[E1, Throwable, T]] =
    for {
      chnk <- query2ChunkMulti(sql)
    } yield ZStream.fromChunk(chnk)
}
