package com.libertexgroup

import com.libertexgroup.algebras.pipelines.Pipeline
import com.libertexgroup.algebras.readers.s3.S3DefaultReader
import com.libertexgroup.algebras.transformers.{NoOpTransformer, Transformer}
import com.libertexgroup.algebras.writers.{DefaultWriter, Writer}
import com.libertexgroup.configs.S3Config
import org.apache.avro.generic.GenericRecord
import software.amazon.awssdk.services.s3.model.S3Exception
import zio.{Console, ExitCode, Layer, Scope, System, ZIO, ZIOAppArgs, ZIOAppDefault, ZLayer}
import zio.s3.{S3, errors}

import java.lang

//object PipelineExample extends ZIOAppDefault {
//  val readText: ZIO[Any with S3, Throwable, Unit] = {
//    val reader = new S3DefaultReader()
//    val transformer: Transformer[S3, GenericRecord, GenericRecord] = new NoOpTransformer[S3, GenericRecord]()
//    val writer: Writer[S3, Console with S3, GenericRecord] = new DefaultWriter[S3, GenericRecord]()
//    (for {
//      _ <- Console.printLine("hello")
//      _ <- Pipeline.apply(reader, transformer, writer)
//    } yield ())
//  }
//
//  val s3ConfigLayer: ZLayer[lang.System, SecurityException, S3Config] = S3Config.live
//  val s3Layer: ZLayer[lang.System, RuntimeException, S3Config with Layer[S3Exception, S3]] =
//    s3ConfigLayer ++ ((s3ConfigLayer >>> S3Config.liveFromS3Config))
//
//  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] =
//    readText.provideLayer(s3Layer).orDie.as(ExitCode.success)
//}