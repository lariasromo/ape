package com.libertexgroup.ape.readers.rest

import com.libertexgroup.ape.readers.Reader

import scala.reflect.ClassTag

abstract class AbstractRestApiReader[E, E1, T :ClassTag] extends Reader[E, E1, T]