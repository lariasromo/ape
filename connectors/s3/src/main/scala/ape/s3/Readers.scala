package ape.s3

import ape.s3.configs.S3Config
import ape.s3.readers.{FileReaderBounded, FileReaderContinuous, FileReaderSimple, S3FileReader}
import ape.utils.Utils.:=
import zio.{Duration, Tag, ZIO}

import java.time.ZonedDateTime

// Readers
protected [s3] class Readers[Config <: S3Config :Tag]() {

  def fileReaderContinuous(lp:ZIO[Config, Nothing, ZonedDateTime => List[String]]): S3FileReader[Config] =
    new FileReaderContinuous(lp)

  def fileReaderSimple(location:String): S3FileReader[Config] =
    new FileReaderSimple[Config](ZIO.succeed((_:ZonedDateTime)=>List(location)))

  def fileReaderBounded(locationPattern:ZIO[Config, Nothing, ZonedDateTime => List[String]],
                        start:ZonedDateTime,  end:ZonedDateTime,
                        step:Duration): S3FileReader[Config] =
    new FileReaderBounded[Config](locationPattern, start, end, step)

}

object Readers {
  def fileReader[Config <: S3Config :Tag](implicit d: Config := S3Config): Readers[Config] = new Readers[Config]
  def contentReader[Config <: S3Config :Tag](reader: S3FileReader[Config])(implicit d: Config := S3Config) =
    new ape.s3.pipes.fromS3Files.Pipes[Config](reader)
}
