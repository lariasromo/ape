package com.libertexgroup.ape.readers.jdbc

import com.libertexgroup.ape.utils.GenericJDBCUtils.query2Chunk
import com.libertexgroup.configs.JDBCConfig
import zio.ZIO
import zio.stream.ZStream

import java.sql.ResultSet
import scala.reflect.ClassTag

class DefaultReader[T: ClassTag](sql:String)
                                (implicit row2Object: ResultSet => T) extends JDBCReader[JDBCConfig, Any, T] {

  override def apply: ZIO[JDBCConfig, Throwable, ZStream[Any, Throwable, T]] = for {
    chnk <- query2Chunk(sql)
  } yield ZStream.fromChunk(chnk)
}
