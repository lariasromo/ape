package ape.s3.pipes.fromS3Files.zioBacked

import ape.reader.Reader
import ape.s3.configs.{CSVConfig, S3Config}
import ape.s3.readers.{S3FileReader, S3FileWithContent}
import com.sksamuel.avro4s.{Decoder, Encoder, SchemaFor}
import purecsv.safe.converter.RawFieldsConverter
import zio.Tag

import scala.reflect.ClassTag

class Pipes[ SConfig <: S3Config :Tag ](reader: S3FileReader[SConfig]) {

  def avro[T>:Null :SchemaFor :Encoder :Decoder :ClassTag :Tag]: Reader[Any, SConfig, S3FileWithContent[T]] =
    Reader.UnitReaderStream(reader --> new AvroPipe[T, SConfig])

  def jsonLines[T>:Null :SchemaFor :Encoder :Decoder :ClassTag :Tag]
    (implicit decode: String => T): Reader[Any, SConfig, S3FileWithContent[T]] =
    Reader.UnitReaderStream(reader --> new JsonLinesPipe[T, SConfig])

  def jsonLinesCirce[T>:Null :SchemaFor :Encoder :Decoder :ClassTag :Tag : io.circe.Decoder]:
    Reader[Any, SConfig, S3FileWithContent[T]] = Reader.UnitReaderStream(reader --> new JsonLinesCircePipe[T, SConfig])

  def text: Reader[Any, SConfig, S3FileWithContent[String]] = Reader.UnitReaderStream(reader --> new TextPipe[SConfig])

  def csv[CsvCfg <: CSVConfig :Tag, T: ClassTag](implicit rfcImp: RawFieldsConverter[T]):
  Reader[Any, CsvCfg with SConfig, S3FileWithContent[T]] = Reader.UnitReaderStream(reader --> new CsvPipe[SConfig, CsvCfg, T])
}