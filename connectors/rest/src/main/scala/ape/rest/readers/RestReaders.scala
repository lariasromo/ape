package ape.rest.readers

import ape.reader.Reader
import zio.http.{Client, Request}

trait RestReaders {
  def byte(request: Request): Reader[Client,Any,Byte]
  def string(request: Request): Reader[Client,Any,String]
}