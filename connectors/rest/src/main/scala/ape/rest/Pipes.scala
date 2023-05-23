package ape.rest

import ape.pipe.Pipe
import ape.rest.pipes._
import ape.utils.Utils.:=
import io.circe.{Decoder, Encoder}
import zio.http.{Client, Request}

import scala.reflect.ClassTag

class Pipes() {
  def byte[ZE]: Pipe[Client,ZE,Request, Byte] = new RestAPIPipeByte[ZE]
  def string[ZE]: Pipe[Client,ZE,Request, String] = new RestAPIPipeString[ZE]

  class decode[ZE] {
    def default[T:ClassTag](implicit enc: String => T): Pipe[Client,ZE,Request, T] =
      new RestAPIPipeDecode[ZE, T]
    def circe[T: Encoder : ClassTag :Decoder]: Pipe[Client,ZE,Request, T] =
      new RestAPIPipeDecodeCirce[ZE, T]
    def zip[T: ClassTag, T2: ClassTag]
    (implicit request: T => Request, t2: String => T2): Pipe[Client, ZE, T, (T, T2)] =
      new RestAPIPipeDecodeZip[ZE, T, T2]
    def zipCirce[T:ClassTag, T2: ClassTag :Decoder]
    (implicit request: T => Request): Pipe[Client, ZE, T, (T, Option[T2])]  =
      new RestAPIPipeDecodeCirceZip[ZE, T, T2]
    def zipCirceWithRequest[T:ClassTag, T2: ClassTag :Decoder]:
      Pipe[Client, ZE, (Request,T),(T, Option[T2])] = new RestAPIPipeRequestCirce[ZE,T,T2]
  }
  def decode[ZE](implicit d1: ZE := Any) = new decode[ZE]
}

object Pipes {
  def pipes = new Pipes()
}
