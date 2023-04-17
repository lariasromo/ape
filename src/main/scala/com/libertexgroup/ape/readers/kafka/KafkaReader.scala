package com.libertexgroup.ape.readers.kafka

import com.libertexgroup.ape.Reader

import scala.reflect.ClassTag

abstract class KafkaReader[E, E1, T :ClassTag] extends Reader[E, E1, T] {
  override val name: String = "KafkaReader"
}