package ape.s3.pipes.fromData

import ape.pipe.Pipe

import scala.reflect.ClassTag


abstract class S3Pipe[E, E1, T :ClassTag, T2 :ClassTag] extends Pipe[E, E1, T, T2]