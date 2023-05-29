package ape.datahub.utils

import ape.datahub.DatahubDataset
import ape.datahub.configs.DatahubConfig
import com.linkedin.common.FabricType
import com.linkedin.common.url.Url
import com.linkedin.common.urn.{DataPlatformUrn, DatasetUrn}
import com.linkedin.dataset.DatasetProperties
import datahub.client.MetadataWriteResponse
import datahub.client.kafka.KafkaEmitter
import datahub.client.rest.RestEmitter
import datahub.event.MetadataChangeProposalWrapper
import zio.ZIO

import scala.concurrent.Future

object DatahubUtils {
  def getMCP[T: DatahubDataset]( fabricType: FabricType = FabricType.DEV ) = {
    val datasetName = implicitly[DatahubDataset[T]].datasetName
    val datasetDescription = implicitly[DatahubDataset[T]].datasetDescription

    val urn: DatasetUrn = new DatasetUrn(
      new DataPlatformUrn("ape"),
      datasetName,
      fabricType
    )

    MetadataChangeProposalWrapper.builder()
      .entityType("dataset")
      .entityUrn(urn)
      .upsert()
      .aspect(new DatasetProperties()
        .setExternalUrl(new Url("https://git.fxclub.org/alexandria/ape"))
        .setDescription(datasetDescription)
      )
      .build();
  }

  def emitRest[T: DatahubDataset]: ZIO[DatahubConfig, Throwable, MetadataWriteResponse] = for {
    dhConfig <- ZIO.service[DatahubConfig]
    f <- {
      val emitter = RestEmitter.create(dhConfig.getRestEmitterConfig)
      val mcp = getMCP[T](dhConfig.fabricType)

      ZIO.fromFuture{ implicit ec =>
        Future {
          emitter.emit(mcp).get()
        }
      }
    }
  } yield f

  def emitKafka[T: DatahubDataset]: ZIO[DatahubConfig, Throwable, MetadataWriteResponse] = for {
    dhConfig <- ZIO.service[DatahubConfig]
    f <- {
      val emitter = new KafkaEmitter(dhConfig.getKafkaEmitterConfig)
      val mcp = getMCP[T](dhConfig.fabricType)
      ZIO.fromFuture{ implicit ec => Future {
        emitter.emit(mcp).get()
      } }
    }
  } yield f
}
