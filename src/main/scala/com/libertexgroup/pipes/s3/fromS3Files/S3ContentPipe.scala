package com.libertexgroup.pipes.s3.fromS3Files

import com.libertexgroup.ape.pipe.Pipe
import com.libertexgroup.readers.s3.S3FileWithContent
import zio.s3.S3ObjectSummary

trait S3ContentPipe[E, ZE, T] extends Pipe[E, ZE, S3ObjectSummary, S3FileWithContent[T]]