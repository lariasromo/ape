package ape.s3.configs
import purecsv.config.Headers.ParseHeaders
import purecsv.config.Trimming.NoAction
import purecsv.config.{Headers, Trimming}
import purecsv.unsafe.RecordSplitter
import zio.{ULayer, ZLayer}

case class CSVConfig(
                      delimiter: Char = RecordSplitter.defaultFieldSeparator,
                      trimming: Trimming = NoAction,
                      headers: Headers = ParseHeaders,
                      headerMapping: Map[String, String] = Map.empty
                    )

object CSVConfig {
  def liveDefault: ULayer[CSVConfig] = ZLayer.succeed(CSVConfig())
}