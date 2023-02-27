package com.libertexgroup.ape.writers.jdbc

import com.libertexgroup.ape.writers.Writer
import zio.ZIO
import zio.stream.ZStream

trait JDBCWriter[E, E1, T] extends Writer[E, E1, T]
