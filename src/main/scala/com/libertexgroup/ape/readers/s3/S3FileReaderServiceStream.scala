package com.libertexgroup.ape.readers.s3

import com.libertexgroup.ape.Ape
import com.libertexgroup.configs.S3Config
import zio.Console.printLine
import zio.s3.{S3, S3ObjectSummary}
import zio.stream.ZStream
import zio.{Queue, Tag, ZIO, ZLayer}

import java.time.ZonedDateTime

protected [s3] class S3FileReaderServiceStream [Config <: S3Config :Tag]
(override val fileStream: ZStream[S3Config with S3, Throwable, S3ObjectSummary])
  extends S3FileReaderService[Config]
object S3FileReaderServiceStream {

  def fin(queue: Queue[S3ObjectSummary]): ZIO[Any, Nothing, Unit] = for {
    _ <- printLine("Shutting down queue").catchAll(e => printLine(e.getMessage).catchAll(_=>ZIO.unit))
    _ <- queue.shutdown
  } yield ()

  def make[Config <: S3Config :Tag](locationPattern: ZIO[Config, Nothing, ZonedDateTime => List[String]]):
  ZIO[S3 with Config, Throwable, S3FileReaderServiceStream[Config]]
  = for {
    queue <- Queue.unbounded[S3ObjectSummary]
    ape <- {
      Ape.readers.s3[Config].fileReaderContinuous(locationPattern) -->
        Ape.writers.misc.queue[Config, S3, S3ObjectSummary](queue)
    }
    _ <- ape.stream.runDrain.ensuring(fin(queue)).fork
    stream = ZStream.fromQueue(queue).tap { file => printLine(s"Getting file ${file.key} from queue") }
  } yield new S3FileReaderServiceStream(stream)


  def live[Config <: S3Config :Tag](locationPattern: ZIO[S3Config, Nothing, ZonedDateTime => List[String]]):
  ZLayer[S3 with Config, Throwable, S3FileReaderServiceStream[Config]]
  = ZLayer.fromZIO(make[Config](locationPattern))
}
