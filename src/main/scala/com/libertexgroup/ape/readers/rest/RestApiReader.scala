package com.libertexgroup.ape.readers.rest



import com.libertexgroup.ape.Reader

import scala.reflect.ClassTag

trait RestApiReader[E, E1, T ] extends Reader[E, E1, T]