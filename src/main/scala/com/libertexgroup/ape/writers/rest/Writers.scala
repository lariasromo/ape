package com.libertexgroup.ape.writers.rest

import io.circe.{Decoder, Encoder}
import zio.http.Request

import scala.reflect.ClassTag

class Writers() {
  def byte[ZE] = new RestAPIWriterByte[ZE]
  def string[ZE] = new RestAPIWriterString[ZE]
  def decode[ZE, T:ClassTag](implicit enc: String => T)  = new RestAPIWriterDecode[ZE, T]
  def decodeCirce[ZE, T: Encoder : ClassTag :Decoder] = new RestAPIWriterDecodeCirce[ZE, T]
  def decodeZip[ZE, T: ClassTag, T2: ClassTag]
  (implicit request: T => Request, t2: String => T2) = new RestAPIWriterDecodeZip[ZE, T, T2]
  def decodeZipCirce[ZE, T:ClassTag, T2: ClassTag :Decoder](implicit request: T => Request) =
    new RestAPIWriterDecodeCirceZip[ZE, T, T2]
}
