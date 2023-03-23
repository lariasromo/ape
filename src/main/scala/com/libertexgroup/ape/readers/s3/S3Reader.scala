package com.libertexgroup.ape.readers.s3

import com.libertexgroup.ape.readers.Reader
import com.libertexgroup.configs.S3Config
import zio.Console.printLine
import zio.{Queue, ZIO}
import zio.s3.{S3, S3ObjectSummary}
import zio.stream.ZStream

import java.io.IOException
import scala.reflect.ClassTag

abstract class S3Reader[E, E1, T :ClassTag] extends Reader[S3FileReaderService with E, E1, T] {
  def fileStream: ZIO[S3FileReaderService, Nothing, ZStream[S3Config with S3, Throwable, S3ObjectSummary]] = for {
    s3FilesQueue <- ZIO.service[S3FileReaderService]
  } yield s3FilesQueue.fileStream
}