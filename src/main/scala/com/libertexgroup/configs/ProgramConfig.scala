package com.libertexgroup.configs

import zio.System.env
import zio.json.{DecoderOps, DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}
import zio.{ZIO, ZLayer}

case class ProgramConfig(
                             reader: String,
                             transformer: String,
                             streamConfig: Option[StreamConfig],
                             writer: String
                           )

object ProgramConfig {
  implicit val decoder: JsonDecoder[ProgramConfig] = DeriveJsonDecoder.gen[ProgramConfig]
  implicit val encoder: JsonEncoder[ProgramConfig] = DeriveJsonEncoder.gen[ProgramConfig]

  def make: ZIO[Any, SecurityException, ProgramConfig] = for {
    reader <- env("READER_NAME")
    transformer <- env("TRANSFORMER_NAME")
    durationMins <- env("DURATION_MINUTES")
    writer <- env("WRITER_NAME")
  } yield ProgramConfig(
    reader = reader.getOrElse(throw new Exception("READER_NAME must be set")),
    streamConfig = durationMins.map(mins => StreamConfig(mins.toLong)),
    transformer = transformer.getOrElse(throw new Exception("TRANSFORMER_NAME must be set")),
    writer = writer.getOrElse(throw new Exception("WRITER_NAME must be set"))
  )

  def fromJsonString(json:String): ZLayer[Any, Throwable, ProgramConfig] = ZLayer {
    ZIO.succeed(json.fromJson[ProgramConfig].getOrElse(throw new Exception("Failed to decode config string")))
  }

  def live: ZLayer[Any, SecurityException, ProgramConfig] = ZLayer(make)
}
