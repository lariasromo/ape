package com.libertexgroup.ape.readers.s3

import com.libertexgroup.ape.Reader
import com.libertexgroup.configs.S3Config
import zio.{Tag, ZIO}
import zio.s3.{S3, S3ObjectSummary}
import zio.stream.ZStream

import scala.reflect.ClassTag

abstract class S3Reader[E, E1, T :ClassTag, AWSS3 <: S3 :Tag, Config <: S3Config :Tag]
  extends Reader[S3FileReaderService[Config, AWSS3] with E, E1, T] {
  def fileStream: ZIO[S3FileReaderService[Config, AWSS3], Nothing, ZStream[Config with AWSS3, Throwable, S3ObjectSummary]] = for {
    s3FilesQueue <- ZIO.service[S3FileReaderService[Config, AWSS3]]
  } yield s3FilesQueue.fileStream
}