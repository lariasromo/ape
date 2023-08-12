package ape.misc.pipes

import ape.pipe.Pipe
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder, jawn}
import zio.{Schedule, ZIO}
import zio.stream.{ZPipeline, ZSink, ZStream}

import java.io.File

class BackPressureDisk[ZE, T: Decoder: Encoder] extends Pipe[Any, ZE, T, T]{
  override protected[this] def pipe(input: ZStream[ZE, Throwable, T]): ZIO[Any, Throwable, ZStream[ZE, Throwable, T]] =
  ZIO.attempt{
    val tmpFile = File.createTempFile("tmp", ".txt")
    val saveLocal = for {
      _ <- input
        .map(_.asJson.noSpaces)
        .map(a => s"${a}\n".getBytes)
        .flatMap(ZStream.fromIterable(_))
        .run(ZSink.fromFile(tmpFile))
        .retry(Schedule.count)
    } yield tmpFile
    ZStream
      .acquireReleaseWith(saveLocal)(file => ZIO.attempt(file.delete()).orDie)
      .flatMap { file =>
        ZStream
          .fromFile(file)
          .via(ZPipeline.utf8Decode >>> ZPipeline.splitLines)
          .map(line => jawn.decode[T](line).toOption)
          .filter(_.isDefined).map(_.get)
      }
  }
}
