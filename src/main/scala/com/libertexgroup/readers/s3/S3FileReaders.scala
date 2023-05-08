package com.libertexgroup.readers.s3

import com.libertexgroup.ape.Ape
import com.libertexgroup.ape.reader.Reader
import com.libertexgroup.configs.S3Config
import zio.Console.printLine
import zio.s3.S3ObjectSummary
import zio.stream.ZStream
import zio.{Duration, Queue, Tag, ZIO}

import java.time.ZonedDateTime

class S3FileReaders[Config <: S3Config :Tag] {
  def bounded(
               locationPattern:ZIO[Config, Nothing, ZonedDateTime => List[String]],
               start:ZonedDateTime,
               end:ZonedDateTime,
               step:Duration
             ): Reader[Config, Any, S3ObjectSummary] =
    Ape.readers.s3[Config].fileReaderBounded(locationPattern, start, end, step)

  def simple(location: String): S3FileReader[Config] = Ape.readers.s3[Config].fileReaderSimple(location)

  def continuous(locationPattern: ZIO[Config, Nothing, ZonedDateTime => List[String]]): Reader[Config, Any, S3ObjectSummary] = {
    new Reader[Config, Any, S3ObjectSummary]{
      override protected[this] def read: ZIO[Config, Throwable, ZStream[Any, Throwable, S3ObjectSummary]] =
        for {
          queue <- Queue.unbounded[S3ObjectSummary]
          _ <- {
            Ape.readers.s3[Config].fileReaderContinuous(locationPattern) -->
              Ape.pipes.misc.queue[Config, Any].of[S3ObjectSummary](queue)
          }.runDrain.ensuring( for {
            _ <- printLine("Shutting down queue").catchAll(_=>ZIO.unit)
            _ <- queue.shutdown
          } yield ()).fork
          stream = ZStream.fromQueue(queue)
        } yield stream
    }
  }
}
