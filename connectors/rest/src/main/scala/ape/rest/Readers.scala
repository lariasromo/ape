package ape.rest

import ape.reader.Reader
import ape.rest.readers.{RestAPIReaderByte, RestAPIReaderString, RestReaders}
import zio.http.{Client, Request}


// Readers
protected [rest] class Readers() extends RestReaders {
  def byte(request: Request): Reader[Client,Any,Byte] = new RestAPIReaderByte(request)

  def string(request: Request): Reader[Client,Any,String] = new RestAPIReaderString(request)
}

object Readers {
  def readers = new Readers()
}
