package ape.rest.readers

import ape.reader.Reader

import scala.reflect.ClassTag

abstract class RestApiReader[E, E1, T: ClassTag] extends Reader[E, E1, T]