package com.libertexgroup.readers.rest

import com.libertexgroup.ape.reader.Reader
import zio.http.{Client, Request}


// Readers
protected [readers] class Readers() extends RestReaders {
  def byte(request: Request): Reader[Client,Any,Byte] = new RestAPIReaderByte(request)

  def string(request: Request): Reader[Client,Any,String] = new RestAPIReaderString(request)
}
