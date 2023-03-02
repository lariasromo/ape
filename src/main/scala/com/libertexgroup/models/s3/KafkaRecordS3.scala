package com.libertexgroup.models.s3

import io.circe.{Json, parser}

import scala.util.Try

case class KafkaRecordS3(
                          message: String,
                          timestamp: String
                        )

object KafkaRecordS3 {
  def decodeCirceString(value: Json, key: String): String = Try {
    ((value \\ key).headOption.map(_.as[String]) match {
      case Some(value) => value match {
        case Left(_) => None
        case Right(value) => Some(value)
      }
      case None => None
    })
  }.toOption.flatten.orNull

  implicit val decoder: String => KafkaRecordS3 = msg => {
    parser.parse(msg) match {
      case Left(_) => None
      case Right(value) => {
        val msg = decodeCirceString(value, "value")
        val timestamp = decodeCirceString(value, "timestamp")
        Some (
          KafkaRecordS3(
            message = msg, timestamp = timestamp
          )
        )
      }
    }
  }.orNull

}