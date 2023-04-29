package com.libertexgroup.ape.readers.s3

import com.libertexgroup.ape.Reader
import com.libertexgroup.configs._
import zio.s3.S3ObjectSummary
import zio.{Duration, Tag, ZIO}

import java.time.ZonedDateTime

// Readers
protected [readers] class Readers[Config <: S3Config :Tag]() {

  def fileReaderContinuous(lp:ZIO[Config, Nothing, ZonedDateTime => List[String]]):
    Reader[Config, Any, S3ObjectSummary] = new FileReaderContinuous(lp)

  def fileReaderSimple(location:String) : FileReaderSimple[Config] =
    new FileReaderSimple[Config](ZIO.succeed((_:ZonedDateTime)=>List(location)))

  def fileReaderBounded(locationPattern:ZIO[Config, Nothing, ZonedDateTime => List[String]],
                              start:ZonedDateTime, end:ZonedDateTime, step:Duration):
  Reader[Config, Any, S3ObjectSummary] = new FileReaderBounded[Config](locationPattern, start, end, step)

}
