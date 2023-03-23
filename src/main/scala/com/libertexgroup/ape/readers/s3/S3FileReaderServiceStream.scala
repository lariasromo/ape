package com.libertexgroup.ape.readers.s3

import com.libertexgroup.ape.pipelines.Pipeline
import com.libertexgroup.configs.S3Config
import zio.Console.printLine
import zio.s3.{S3, S3ObjectSummary}
import zio.stream.ZStream
import zio.{Queue, Scope, ZIO, ZLayer}

import java.io.IOException
import java.time.ZonedDateTime

class S3FileReaderServiceStream (override val fileStream: ZStream[S3Config with S3, Throwable, S3ObjectSummary]) extends S3FileReaderService
object S3FileReaderServiceStream {

  def fin(queue: Queue[S3ObjectSummary]): ZIO[Any, Nothing, Unit] = for {
    _ <- printLine("Shutting down queue").catchAll(e => printLine(e.getMessage).catchAll(_=>ZIO.unit))
    _ <- queue.shutdown
  } yield ()

  def make(locationPattern: ZIO[S3Config, Nothing, ZonedDateTime => List[String]]): ZIO[S3Config with S3, Nothing, S3FileReaderServiceStream]
  = for {
    queue <- Queue.unbounded[S3ObjectSummary]
    pipe = Pipeline.readers.s3FileReaderContinuous(locationPattern) --> Pipeline.writers.queueWriter(queue)
    _ <- pipe.run.ensuring(fin(queue)).fork
    stream = ZStream.fromQueue(queue).tap { file => printLine(s"Getting file ${file.key} from queue") }
  } yield new S3FileReaderServiceStream(stream)


  def live(locationPattern: ZIO[S3Config, Nothing, ZonedDateTime => List[String]]):
  ZLayer[S3Config with S3 with Scope, Nothing, S3FileReaderServiceStream] = ZLayer.fromZIO(make(locationPattern))
}
