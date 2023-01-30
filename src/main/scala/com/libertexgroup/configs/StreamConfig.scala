package com.libertexgroup.configs

import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}

case class StreamConfig (
                        durationMinutes: Long
                        )

object StreamConfig {
  implicit val decoder: JsonDecoder[StreamConfig] = DeriveJsonDecoder.gen[StreamConfig]
  implicit val encoder: JsonEncoder[StreamConfig] = DeriveJsonEncoder.gen[StreamConfig]

}