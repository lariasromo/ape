package com.libertexgroup.pipes.redis.backPressure

import com.libertexgroup.ape.pipe.Pipe
import com.libertexgroup.configs.RedisConfig
import com.libertexgroup.utils.Utils.:=
import com.sksamuel.avro4s.{Decoder, Encoder, SchemaFor}
import zio.Tag

import scala.reflect.ClassTag

class Pipes[Config <: RedisConfig : Tag] {
  class finite[E, ZE] {
    def typed[T :SchemaFor :Encoder :Decoder :ClassTag :Tag]: Pipe[E with ZE with Config, ZE, T, T] =
      new BackPressureFinitePipe[E, ZE, Config, T]()

    def string: Pipe[E with ZE with Config, ZE, String, String] = new BackPressureStringFinitePipe[E, ZE, Config]()
  }
  def finite[E, ZE](implicit d: E := Any, d1: ZE := Any, d2: Config := RedisConfig) = new finite[E, ZE]

  class infinite[E, ZE] {
    def typed[T :SchemaFor :Encoder :Decoder :ClassTag :Tag]: Pipe[E with ZE with Config, ZE, T, T] =
      new BackPressureInfinitePipe[E, ZE, Config, T]()

    def string: Pipe[E with ZE with Config, ZE, String, String] = new BackPressureInfiniteStringPipe[E, ZE, Config]()
  }
  def infinite[E, ZE](implicit d: E := Any, d1: ZE := Any, d2: Config := RedisConfig) = new infinite[E, ZE]
}