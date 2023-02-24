package com.libertexgroup.ape.readers.s3

import com.libertexgroup.ape.readers.Reader

import scala.reflect.ClassTag

abstract class S3Reader[E, E1, T :ClassTag] extends Reader[E, E1, T]