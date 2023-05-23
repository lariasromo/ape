package ape.s3

import ape.s3.configs.S3Config
import ape.s3.readers.S3FileReader
import ape.utils.Utils.:=
import zio.Tag

object Pipes {
  def fromData[Config <: S3Config : Tag](implicit d: Config := S3Config) = new ape.s3.pipes.fromData.Pipes[Config]

  def fromS3Files[Config <: S3Config : Tag](reader: S3FileReader[Config])(implicit d: Config := S3Config) =
    new pipes.fromS3Files.Pipes[Config](reader)
}
