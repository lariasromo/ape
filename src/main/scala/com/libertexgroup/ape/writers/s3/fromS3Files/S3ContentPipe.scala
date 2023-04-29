package com.libertexgroup.ape.writers.s3.fromS3Files

import com.libertexgroup.ape.Writer
import com.libertexgroup.ape.readers.s3.S3FileWithContent
import zio.s3.S3ObjectSummary

trait S3ContentPipe[E, ZE, T] extends Writer[E, ZE, S3ObjectSummary, S3FileWithContent[T]]