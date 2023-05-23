package ape.kafka.readers

import ape.reader.Reader

import scala.reflect.ClassTag

abstract class KafkaReader[E, E1, T :ClassTag] extends Reader[E, E1, T]