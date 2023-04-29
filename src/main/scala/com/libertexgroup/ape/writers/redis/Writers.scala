package com.libertexgroup.ape.writers.redis

import com.libertexgroup.ape.Writer
import com.libertexgroup.configs.RedisConfig
import com.sksamuel.avro4s.{Decoder, Encoder, SchemaFor}
import zio.Tag

import scala.reflect.ClassTag

object Writers {
  class Writers[Config <: RedisConfig : Tag]{
    def generalPurpose = new GeneralPurposeWriters[Config]
    def backPressure = new BackPressureWriters[Config]
  }

  class GeneralPurposeWriters[Config <: RedisConfig : Tag] {
    // general purpose
    def avro[ZE, T: ClassTag : Tag : SchemaFor : Encoder](queueName: String): Writer[Config, ZE, T, T] = new AvroWriter[ZE, Config, T](queueName)

    def default[ZE, T: ClassTag : Tag : SchemaFor : Encoder](queueName: String): Writer[Config, ZE, T, T] = avro[ZE, T](queueName)

    def string[ZE](queueName: String): Writer[Config, ZE, String, String] = new StringWriter[ZE, Config](queueName)

    // back pressure (to use with other readers/writers)
    def backPressure = new BackPressureWriters
  }

  class BackPressureWriters[Config <: RedisConfig : Tag] {
    def finite[ZE, T>:Null :SchemaFor :Encoder :Decoder :ClassTag :Tag]: Writer[ZE with Config, ZE, T, T] =
      new BackPressureFiniteWriter[ZE, Config, T]()

    def infinite[ZE, T>:Null :SchemaFor :Encoder :Decoder :ClassTag :Tag]: Writer[ZE with Config, ZE, T, T] =
      new BackPressureInfiniteWriter[ZE, Config, T]()
  }
}
