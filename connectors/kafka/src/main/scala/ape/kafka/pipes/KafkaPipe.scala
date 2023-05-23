package ape.kafka.pipes

import ape.pipe.Pipe
import org.apache.kafka.clients.producer.ProducerRecord

abstract class KafkaPipe[E, E1, T1, T2] extends Pipe[E, E1, ProducerRecord[T1, T2], ProducerRecord[T1, T2]]