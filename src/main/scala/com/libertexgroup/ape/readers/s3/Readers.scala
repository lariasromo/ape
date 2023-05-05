package com.libertexgroup.ape.readers.s3

import com.libertexgroup.configs._
import zio.{Duration, Tag, ZIO}

import java.time.ZonedDateTime

// Readers
protected [readers] class Readers[Config <: S3Config :Tag]() {

  def fileReaderContinuous(lp:ZIO[Config, Nothing, ZonedDateTime => List[String]]) =
    new FileReaderContinuous(lp)

  def fileReaderSimple(location:String) : FileReaderSimple[Config] =
    new FileReaderSimple[Config](ZIO.succeed((_:ZonedDateTime)=>List(location)))

  def fileReaderBounded(locationPattern:ZIO[Config, Nothing, ZonedDateTime => List[String]],
                              start:ZonedDateTime, end:ZonedDateTime, step:Duration) =
    new FileReaderBounded[Config](locationPattern, start, end, step)

}
