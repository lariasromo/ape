package com.libertexgroup.ape.writers.rest

import com.libertexgroup.ape.Writer
import io.circe.{Decoder, Encoder}
import zio.http.{Client, Request}

import scala.reflect.ClassTag

class Writers() {
  def byte[ZE]: Writer[Client,ZE,Request, Byte] = new RestAPIWriterByte[ZE]
  def string[ZE]: Writer[Client,ZE,Request, String] = new RestAPIWriterString[ZE]
  def decode[ZE, T:ClassTag](implicit enc: String => T): Writer[Client,ZE,Request, T] =
    new RestAPIWriterDecode[ZE, T]
  def decodeCirce[ZE, T: Encoder : ClassTag :Decoder]: Writer[Client,ZE,Request, T] =
    new RestAPIWriterDecodeCirce[ZE, T]
  def decodeZip[ZE, T: ClassTag, T2: ClassTag]
  (implicit request: T => Request, t2: String => T2): Writer[Client, ZE, T, (T, T2)] =
    new RestAPIWriterDecodeZip[ZE, T, T2]
  def decodeZipCirce[ZE, T:ClassTag, T2: ClassTag :Decoder]
  (implicit request: T => Request): Writer[Client, ZE, T, (T, Option[T2])]  =
    new RestAPIWriterDecodeCirceZip[ZE, T, T2]
}
