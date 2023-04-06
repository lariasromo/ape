package com.libertexgroup

import com.libertexgroup.ape.Ape
import com.libertexgroup.ape.readers.s3.{S3FileReaderService, S3FileReaderServiceBounded}
import com.libertexgroup.ape.utils.S3Utils
import com.libertexgroup.configs.S3Config
import com.libertexgroup.models.s3.CompressionType
import zio.s3.S3
import zio.stream.ZSink
import zio.{Scope, ZIO, ZIOAppArgs, ZIOAppDefault, ZLayer, durationInt}

import java.time.ZonedDateTime

object ex extends ZIOAppDefault{
  val location = "topics/ab-service--determination"
  val readerService: ZLayer[S3Config with S3, Throwable, S3FileReaderService[S3Config]] =
    S3FileReaderServiceBounded.live(
      S3Utils.pathConverter(location),
      ZonedDateTime.now().minusHours(10),
      ZonedDateTime.now().minusHours(1),
      1.hour
    )

  val layer: ZLayer[Any, Throwable, S3 with S3Config with S3FileReaderService[S3Config]] =
    (S3Config.liveS3Default ++ ZLayer.succeed(
      S3Config(
        compressionType = CompressionType.GZIP,
        parallelism = 1,
        enableBackPressure = false,
        s3Bucket = Some("ltx-eu-west-1-datalake-bronze")
      )
    )) >+> readerService

  val lookup = Seq(
    "active_group",
//    "Active_Cysec_Volatile_Assets_Notification_Test_C",
//    "24492121", "24502211", "24641651"
  )

  val main: ZIO[S3 with S3Config with S3FileReaderService[S3Config] with Any, Throwable, Unit] = for {
    p <- Ape.readers.s3[S3Config].text
      .mapZ(r => r.map(_._2).flatMap(x=>x))
      .mapZ(s => s.filter(r => lookup.exists(r.contains(_)))).apply
    data <- p
      .map(value => value.toByte)
      .run(ZSink.fromFile(new java.io.File("/tmp/file.txt")))
  } yield data

  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] = main.provideSomeLayer(layer)
}
