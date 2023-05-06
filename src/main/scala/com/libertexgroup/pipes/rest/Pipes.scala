package com.libertexgroup.pipes.rest

import com.libertexgroup.ape.pipe.Pipe
import io.circe.{Decoder, Encoder}
import zio.http.{Client, Request}

import scala.reflect.ClassTag

class Pipes() {
  def byte[ZE]: Pipe[Client,ZE,Request, Byte] = new RestAPIPipeByte[ZE]
  def string[ZE]: Pipe[Client,ZE,Request, String] = new RestAPIPipeString[ZE]
  def decode[ZE, T:ClassTag](implicit enc: String => T): Pipe[Client,ZE,Request, T] =
    new RestAPIPipeDecode[ZE, T]
  def decodeCirce[ZE, T: Encoder : ClassTag :Decoder]: Pipe[Client,ZE,Request, T] =
    new RestAPIPipeDecodeCirce[ZE, T]
  def decodeZip[ZE, T: ClassTag, T2: ClassTag]
  (implicit request: T => Request, t2: String => T2): Pipe[Client, ZE, T, (T, T2)] =
    new RestAPIPipeDecodeZip[ZE, T, T2]
  def decodeZipCirce[ZE, T:ClassTag, T2: ClassTag :Decoder]
  (implicit request: T => Request): Pipe[Client, ZE, T, (T, Option[T2])]  =
    new RestAPIPipeDecodeCirceZip[ZE, T, T2]
  def decodeZipCirceWithRequest[E, ZE, T:ClassTag, T2: ClassTag :Decoder] : Pipe[E with Client, ZE, (Request,T),(T,Option[T2])] =
    new RestAPIPipeRequestCirce[E,ZE,T,T2]
}
