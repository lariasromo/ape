package com.libertexgroup.configs

import zio.json.{DecoderOps, DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}
import zio.{Has, ZIO, ZLayer, system}

case class StreamConfig (
                        durationMinutes: Long
                        )

object StreamConfig {
  implicit val decoder: JsonDecoder[StreamConfig] = DeriveJsonDecoder.gen[StreamConfig]
  implicit val encoder: JsonEncoder[StreamConfig] = DeriveJsonEncoder.gen[StreamConfig]

}