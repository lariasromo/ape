package com.libertexgroup.ape.writers.s3

import com.libertexgroup.ape.readers.s3.S3FileReader
import com.libertexgroup.configs.S3Config
import zio.Tag

class Writers[Config <: S3Config :Tag] {
  def fromData = new com.libertexgroup.ape.writers.s3.fromData.Writers[Config]
  def fromS3Files[ZE](reader: S3FileReader[Config]) =
    new com.libertexgroup.ape.writers.s3.fromS3Files.Writers[Any, Config](reader)
}
