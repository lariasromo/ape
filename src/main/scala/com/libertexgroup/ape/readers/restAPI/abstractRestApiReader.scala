package com.libertexgroup.ape.readers.restAPI

import com.libertexgroup.ape.readers.Reader

import scala.reflect.ClassTag

abstract class abstractRestApiReader[E, E1, T :ClassTag] extends Reader[E, E1, T]