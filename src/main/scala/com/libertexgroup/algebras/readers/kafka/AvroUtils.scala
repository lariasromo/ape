package com.libertexgroup.algebras.readers.kafka

import com.sksamuel.avro4s._
import org.apache.avro.file.SeekableByteArrayInput
import org.apache.avro.generic.{GenericDatumReader, GenericRecord}
import org.apache.avro.io.DecoderFactory
import zio.kafka.serde.Serde

import java.io.ByteArrayOutputStream
import scala.collection.mutable
import scala.util.{Failure, Success, Try}

object AvroUtils {
  def getSerde[T>:Null: SchemaFor: Decoder: Encoder]: Serde[Any, Option[T]] = Serde.byteArray.inmap {
    matchAsByteArray => {
      val decoder = new DecoderImplicit(matchAsByteArray)
      decoder.decode[T]().headOption
    }
  } {
    matchAsObj => {
      matchAsObj.flatMap(obj => {
        new EncoderImplicit( List(obj) ).encode[T]()
      }).getOrElse(Array.emptyByteArray)
    }
  }

  object implicits {
    implicit val anyToAvro: Any => EncoderImplicit = l => new EncoderImplicit(Seq(l))
    implicit val listToAvro: List[Any] => EncoderImplicit = l => new EncoderImplicit(l)
    implicit val seqToAvro: Seq[Any] => EncoderImplicit = s => new EncoderImplicit(s)
    implicit val bytesToSeq: Array[Byte] => DecoderImplicit = ba => new DecoderImplicit(ba)
  }

  class DecoderImplicit(bytes: Array[Byte]) {
    def readBinary(bytes: Array[Byte], schema: org.apache.avro.Schema): List[GenericRecord] = {

      val datumReader = new GenericDatumReader[GenericRecord](schema)
      val inputStream = new SeekableByteArrayInput(bytes)
      val decoder = DecoderFactory.get.binaryDecoder(inputStream, null)

      var result: List[GenericRecord] = List.empty
      while (!decoder.isEnd) {
        Try(datumReader.read(null, decoder)) match {
          case Failure(exception) => {
            exception.printStackTrace()
          }
          case Success(value) =>
            result = result ++ List(value)
        }
      }
      result
    }
    def decode[T>:Null: SchemaFor : Decoder : Encoder](): List[T] = {
      val result = Try({
        val schema = AvroSchema[T]
        readBinary(bytes, schema)
      }) match {
        case Failure(exception) => {
          exception.printStackTrace()
          None
        }
        case Success(result) => {
          Some(result.map(RecordFormat[T].from))
        }
      }
      result.getOrElse(List.empty[T])
    }
  }

  class EncoderImplicit(data: Seq[Any]) {
    def encode[T: SchemaFor : Encoder](): Option[Array[Byte]] = {
      val baos = new ByteArrayOutputStream()
      Try(AvroOutputStream.data[T].to(baos).build()) match {
        case Failure(exception) => {
          exception.printStackTrace()
          None
        }
        case Success(os) => {
          os.write(data.map(_.asInstanceOf[T]))
          os.flush()
          os.close()
          Some(baos.toByteArray)
        }
      }
    }
  }
}
