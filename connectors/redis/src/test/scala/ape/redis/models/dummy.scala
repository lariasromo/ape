package ape.redis.models

import io.circe._
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.parser._
import io.circe.syntax.EncoderOps

import java.sql.ResultSet

case class dummy(a: String, b: String) {
  override def toString: String = s"$a, $b"
}

object dummy {
  implicit val jsonDecoder: Decoder[dummy] = deriveDecoder[dummy]
  implicit val jsonEncoder: Encoder[dummy] = deriveEncoder[dummy]

  implicit val str2Dummy: String => dummy = s => decode[dummy](s).toOption.orNull
  implicit val dummy2Str: dummy => String = s => s.asJson.noSpaces

  implicit val row2Object: ResultSet => dummy = rs => {
    dummy(
      rs.getString("a"),
      rs.getString("b")
    )
  }
}
