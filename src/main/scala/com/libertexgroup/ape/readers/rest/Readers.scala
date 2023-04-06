package com.libertexgroup.ape.readers.rest

import com.libertexgroup.ape
import com.libertexgroup.ape.Reader
import zio.http.{Client, Request}

// Readers
protected [readers] class Readers() {
  def byte[E](request: Request): Reader[Client,E,Byte] = new RestAPIReaderByte[E](request)

  def string[E](request: Request): Reader[Client,E,String] = new RestAPIReaderString[E](request)
}
