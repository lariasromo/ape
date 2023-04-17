package com.libertexgroup.ape.readers.jdbc

import com.libertexgroup.ape.utils.GenericJDBCUtils.query2Chunk
import com.libertexgroup.configs.JDBCConfig
import zio.{Tag, ZIO}
import zio.stream.ZStream

import java.sql.ResultSet
import scala.reflect.ClassTag

protected[jdbc] class DefaultReader[E, T: ClassTag, Config <: JDBCConfig :Tag](sql:String)(implicit r: ResultSet => T)
  extends JDBCReader[Config, E, T] {

  override protected[this] def read: ZIO[Config, Throwable, ZStream[E, Throwable, T]] =
      for {
        chnk <- query2Chunk[T, Config](sql)
      } yield ZStream.fromChunk(chnk)
}
