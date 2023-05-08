package com.libertexgroup.readers.rest

import com.libertexgroup.ape.reader.Reader
import zio.http.{Client, Request}

trait RestReaders {
  def byte[E](request: Request): Reader[Client,E,Byte]
  def string[E](request: Request): Reader[Client,E,String]
}