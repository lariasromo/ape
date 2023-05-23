package ape.s3.pipes.fromS3Files

import ape.pipe.Pipe
import ape.s3.readers.S3FileWithContent
import zio.s3.S3ObjectSummary

trait S3ContentPipe[E, ZE, T] extends Pipe[E, ZE, S3ObjectSummary, S3FileWithContent[T]]
