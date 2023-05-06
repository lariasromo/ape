package com.libertexgroup.pipes.redis.backPressure

import com.libertexgroup.ape.pipe.Pipe
import com.libertexgroup.configs.RedisConfig
import com.sksamuel.avro4s.{Decoder, Encoder, SchemaFor}
import zio.Tag

import scala.reflect.ClassTag

class Pipes[Config <: RedisConfig : Tag] {
  def finite[ZE, T :SchemaFor :Encoder :Decoder :ClassTag :Tag]: Pipe[ZE with Config, ZE, T, T] =
    new BackPressureFinitePipe[ZE, Config, T]()

  def finiteString[ZE]: Pipe[ZE with Config, ZE, String, String] = new BackPressureStringFinitePipe[ZE, Config]()

  def infinite[ZE, T :SchemaFor :Encoder :Decoder :ClassTag :Tag]: Pipe[ZE with Config, ZE, T, T] =
    new BackPressureInfinitePipe[ZE, Config, T]()

  def infiniteString[ZE]: Pipe[ZE with Config, ZE, String, String] =
    new BackPressureInfiniteStringPipe[ZE, Config]()
}