package com.libertexgroup.ape.readers.s3

import com.libertexgroup.configs.S3Config
import zio.s3.{S3, S3ObjectSummary}
import zio.stream.ZStream

trait S3FileReaderService {
  val fileStream: ZStream[S3Config with S3, Throwable, S3ObjectSummary]
}
