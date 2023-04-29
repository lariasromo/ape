package com.libertexgroup.ape.readers.s3

import com.libertexgroup.ape.writers.s3.fromS3Files.Writers
import com.libertexgroup.ape.{Ape, Reader}
import com.libertexgroup.configs.S3Config
import zio.Tag
import zio.s3.S3ObjectSummary


abstract class S3FileReader[Config <: S3Config :Tag, ZE] extends Reader[Config, ZE, S3ObjectSummary]{
  def readFiles: Writers[ZE, Config] = Ape.writers.s3[Config].fromS3Files(this)
}
