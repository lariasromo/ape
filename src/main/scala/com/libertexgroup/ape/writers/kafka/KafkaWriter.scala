package com.libertexgroup.ape.writers.kafka

import com.libertexgroup.ape.writers.Writer
import org.apache.kafka.clients.producer.ProducerRecord
import zio.ZIO
import zio.stream.ZStream

trait KafkaWriter[E, E1, T1, T2] extends Writer[E, E1, ProducerRecord[T1, T2]]
