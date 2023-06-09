//package ape.utils
//
//import com.sksamuel.avro4s._
//import org.apache.avro.Schema
//import org.apache.avro.generic.GenericRecord
//import org.apache.hadoop.conf.Configuration
//import org.apache.hadoop.fs.Path
//import org.apache.parquet.avro.{AvroParquetReader, AvroParquetWriter}
//import org.apache.parquet.hadoop.util.HadoopInputFile
//import org.apache.parquet.io.OutputFile
//import zio.s3.S3ObjectSummary
//import zio.stream.ZStream
//import zio.{Chunk, ZIO}
//
//import java.io.{BufferedOutputStream, ByteArrayOutputStream}
//
//object ParquetUtils {
//  def readParquetGenericRecord(file: S3ObjectSummary): ZStream[Any, Nothing, GenericRecord] = {
//    val path = new Path(s"s3a://${file.bucketName}/${file.key}")
//    val conf = new Configuration()
//    //    conf.set("fs.s3a.access.key", config.awsAccessKey)
//    //    conf.set("fs.s3a.secret.key", config.awsSecretKey)
//    conf.set("fs.s3a.impl", "org.apache.hadoop.fs.s3a.S3AFileSystem")
//    //    if(config.s3Host.isDefined) {
//    //      conf.set("fs.s3a.endpoint", config.s3Host.get)
//    //    }
//    conf.setBoolean("fs.s3a.path.style.access", true)
//    conf.setBoolean(org.apache.parquet.avro.AvroReadSupport.READ_INT96_AS_FIXED, true)
//
//    ZStream.acquireReleaseWith(
//      ZIO.succeed(AvroParquetReader.builder[GenericRecord](
//        HadoopInputFile.fromPath(path, conf)
//      ).build)
//    )(x => ZIO.succeed(x.close()))
//      .flatMap { is =>
//        ZStream.succeed(is.read())
//      }
//  }
//
//  def readParquetWithType[T >: Null : SchemaFor : Encoder : Decoder](file: S3ObjectSummary): ZStream[Any, Nothing, T] = {
//    val path = new Path(s"s3a://${file.bucketName}/${file.key}")
//    val conf = new Configuration()
//    //    conf.set("fs.s3a.access.key", config.awsAccessKey)
//    //    conf.set("fs.s3a.secret.key", config.awsSecretKey)
//    conf.set("fs.s3a.impl", "org.apache.hadoop.fs.s3a.S3AFileSystem")
//    //    if(config.s3Host.isDefined) {
//    //      conf.set("fs.s3a.endpoint", config.s3Host.get)
//    //    }
//    conf.setBoolean("fs.s3a.path.style.access", true)
//    conf.setBoolean(org.apache.parquet.avro.AvroReadSupport.READ_INT96_AS_FIXED, true)
//
//    val hp = HadoopInputFile.fromPath(path, conf)
//    ZStream.acquireReleaseWith(
//      ZIO.succeed(
//        AvroParquetReader.builder[Record](hp).build()
//      )
//    )(x => ZIO.succeed(x.close()))
//      .flatMap { is => {
//        val read: Record = is.read()
//        val entity: T = FromRecord.apply[T].from(read)
//        //val record = FromRecord.apply[T].from(read)
//        ZStream.succeed(entity)
//      }
//      }
//  }
//
//
//  def recordsToParquetBytes[T >: Null : SchemaFor : Encoder : Decoder](records: Chunk[T]): ZIO[Any, Nothing, Array[Byte]] = {
//    val stream = new ByteArrayOutputStream();
//    val buffer = new BufferedOutputStream(stream);
//    val out: OutputFile = new ParquetBufferedWriter(buffer);
//    val schema: Schema = AvroSchema[T]
//    val writer = AvroParquetWriter.builder[GenericRecord](out)
//      .withPageSize(1024)
//      .withSchema(schema)
//      .build()
//
//    ZIO.scoped {
//      ZIO.acquireRelease(
//        for {
//          _ <- ZIO.foreach(records) { o =>
//            ZIO.succeed {
//              val record: Record = ToRecord.apply[T].to(o)
//              writer.write(record)
//            }
//          }
//          _ <- ZIO.succeed(buffer.flush())
//          _ <- ZIO.succeed(writer.close())
//        } yield stream.toByteArray
//      )(_ => ZIO.succeed {
//        stream.close()
//        buffer.close()
//      })
//    }
//  }
//}
