package ape.s3.pipes.fromS3Files.noneBacked

import ape.reader.Reader
import ape.s3.configs.{CSVConfig, S3Config}
import ape.s3.readers.{S3FileReader, S3FileWithContent}
import com.sksamuel.avro4s.{Decoder, Encoder, SchemaFor}
import purecsv.safe.converter.RawFieldsConverter
import zio.Tag

import scala.reflect.ClassTag

class Pipes[ SConfig <: S3Config :Tag ](reader: S3FileReader[SConfig]) {

  def avro[T :SchemaFor :Encoder :Decoder :ClassTag :Tag]: Reader[Any, SConfig, S3FileWithContent[T]] =
    Reader.UnitReader(reader --> new AvroPipe[T, SConfig])

  def jsonLines[T :SchemaFor :Encoder :Decoder :ClassTag :Tag]
    (implicit decode: String => T): Reader[Any, SConfig, S3FileWithContent[T]] =
    Reader.UnitReader(reader --> new JsonLinesPipe[T, SConfig])

  def jsonLinesCirce[T  :SchemaFor :Encoder :Decoder :ClassTag :Tag : io.circe.Decoder]:
    Reader[Any, SConfig, S3FileWithContent[T]] = Reader.UnitReader(reader --> new JsonLinesCircePipe[T, SConfig])

  def text: Reader[Any, SConfig, S3FileWithContent[String]] = Reader.UnitReader(reader --> new TextPipe[SConfig])

  def csv[ZE, CsvCfg <: CSVConfig :Tag, T: ClassTag](implicit rfcImp: RawFieldsConverter[T]):
    Reader[Any, SConfig with CsvCfg, S3FileWithContent[T]] =
    Reader.UnitReader(reader --> new CsvPipe[SConfig, CsvCfg, T])
}