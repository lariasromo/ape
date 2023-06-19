package ape.datahub.utils

import ape.datahub.configs.DatahubConfig
import ape.datahub.pipe.EmitterMechanism
import ape.datahub.pipe.EmitterMechanism.EmitterMechanism
import com.linkedin.common.urn.{DataPlatformUrn, DatasetUrn, TagUrn}
import com.linkedin.common.{FabricType, GlobalTags, TagAssociation, TagAssociationArray}
import com.linkedin.data.template.SetMode
import com.linkedin.dataset.{DatasetLineageType, Upstream, UpstreamArray, UpstreamLineage}
import com.linkedin.schema.SchemaFieldDataType.Type
import com.linkedin.schema.SchemaMetadata.PlatformSchema
import com.linkedin.schema._
import com.sksamuel.avro4s.{AvroSchema, SchemaFor}
import datahub.client.kafka.KafkaEmitter
import datahub.client.rest.RestEmitter
import datahub.client.{Emitter, MetadataWriteResponse}
import datahub.event.MetadataChangeProposalWrapper
import datahub.event.MetadataChangeProposalWrapper.MetadataChangeProposalWrapperBuilder
import org.apache.avro.Schema
import org.apache.avro.Schema.Type._
import zio.ZIO

import scala.concurrent.Future
import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.reflect.{ClassTag, classTag}

object DatahubUtils {

  def createDataset[A :SchemaFor :ClassTag]: ZIO[DatahubConfig, Throwable, DatasetUrn] = for {
    _ <-  ZIO.logInfo("Dataset Name: " + classTag[A].runtimeClass.getSimpleName)
    resp <- DatahubUtils.emitDataset[A]
    (urn, emitResp) = resp
    _ <- if(emitResp.isSuccess){
      ZIO.logInfo("Successfully emitted metadata event") *>
        ZIO.logInfo(emitResp.toString) *>
        ZIO.logInfo(emitResp.getResponseContent) *> ZIO.unit
    } else {
      ZIO.logError("Failed to emit metadata event") *>
        ZIO.logError(emitResp.getUnderlyingResponse.toString) *>
        ZIO.attempt(throw new Exception("Failed to emit datahub dataset"))
    }
  } yield urn

  def createLineage(upstreams:Seq[DatasetUrn], downstream:DatasetUrn): ZIO[DatahubConfig, Throwable, MetadataWriteResponse] = for {
    lineageResp <- DatahubUtils.emitLineage(upstreams, downstream)
    _ <- if(lineageResp.isSuccess){
      ZIO.logInfo("Successfully emitted metadata lineage event") *>
        ZIO.logInfo(lineageResp.toString) *>
        ZIO.logInfo(lineageResp.getResponseContent) *> ZIO.unit
    } else {
      ZIO.logError("Failed to emit metadata lineage event") *>
        ZIO.logError(lineageResp.getUnderlyingResponse.toString) *>
        ZIO.attempt(throw new Exception("Failed to emit datahub lineage"))
    }
  } yield lineageResp

  def avro2LinkedInType(f: Schema.Field): Type = {
    f.schema().getType match {
      case RECORD => Type.create(new RecordType())
      case ENUM => Type.create(new EnumType())
      case ARRAY => Type.create(new ArrayType())
      case MAP => Type.create(new MapType())
      case UNION => Type.create(new MapType())
      case FIXED => Type.create(new FixedType())
      case STRING => Type.create(new StringType())
      case BYTES => Type.create(new BytesType())
      case INT | LONG | FLOAT | DOUBLE => Type.create(new NumberType())
      case BOOLEAN => Type.create(new BooleanType())
      case NULL => Type.create(new NullType())
    }
  }

  def datahubFields[T: ClassTag:SchemaFor]: SchemaFieldArray = {
    val avroSchema = AvroSchema[T]
    //    val types = avroSchema.getTypes
    val fields: Iterable[SchemaField] = avroSchema
      .getFields
      .asScala
      .map(field => {
        val t = avro2LinkedInType(field)
        val dtf = new SchemaFieldDataType().setType(t)
        new SchemaField().setType(dtf).setFieldPath(field.name()).setNativeDataType(field.schema().getName)
      })
    var fieldArray = new SchemaFieldArray()
    fields.foreach(fieldArray.add)
    fieldArray
  }

  def getLineageChangeProposal(
                                upstreams: Seq[DatasetUrn],
                                downstream: DatasetUrn
                              ) = {
    var upstreamTables = new UpstreamArray()
    upstreams
      .map(urn => {
        new Upstream().setDataset(urn, SetMode.IGNORE_NULL).setType(DatasetLineageType.TRANSFORMED)
      })
      .foreach(upstreamTables.add)

    val upstreamLineage = new UpstreamLineage().setUpstreams(upstreamTables)

    val mcp = MetadataChangeProposalWrapper
      .builder()
      .entityType("dataset")
      .entityUrn(downstream)
      .upsert()
      .aspect(upstreamLineage)
      .build()

    Seq(mcp)
  }

  def getDatasetChangeProposal[T: ClassTag :SchemaFor] (
                                                         fabricType: FabricType = FabricType.DEV,
                                                         tags:Seq[String] = Seq.empty
                                                       ) =
  {

    val datasetName = classTag[T].runtimeClass.getSimpleName
    val urn: DatasetUrn = new DatasetUrn(new DataPlatformUrn("ape"), datasetName, fabricType)
    val avroSchema = AvroSchema[T]
    val fields = datahubFields[T]
    val schemaStr: String = avroSchema.toString(false)
    val schema = PlatformSchema.create(new BinaryJsonSchema().setSchema(schemaStr))

    val aspectSchema = new SchemaMetadata()
      .setDataset(urn)
      .setFields(fields)
      .setHash("")
      .setPlatform(new DataPlatformUrn("ape"))
      .setPlatformSchema(schema)
      .setSchemaName(avroSchema.getNamespace + "." + datasetName)
      .setVersion(0)

    val tagArray = new TagAssociationArray()
    tags.foreach(t => tagArray.add(new TagAssociation().setTag(new TagUrn(t))))

    val aspectTags = new GlobalTags().setTags(tagArray)

    val c = MetadataChangeProposalWrapper
      .builder()
      .entityType("dataset")
      .entityUrn(urn)
      .upsert()

    val c1 = c.aspect(aspectSchema).build()
    val c2 = c.aspect(aspectTags).build()
    (urn, Seq(c1, c2))
  }

  def emitLineage(upstreams: Seq[DatasetUrn], downstream:DatasetUrn):
    ZIO[DatahubConfig, Throwable, MetadataWriteResponse] =
      for {
        emitter <- getEmitter
        resp <- getLineageChangeProposal(upstreams, downstream)
          .map(mcp => ZIO.fromFuture{ implicit ec => Future(emitter.emit(mcp).get()) })
          .reduce(_ *> _)
      } yield resp

  def emitDataset[T: ClassTag :SchemaFor]: ZIO[DatahubConfig, Throwable, (DatasetUrn, MetadataWriteResponse)] =
    for {
      dhConfig <- ZIO.service[DatahubConfig]
      emitter <- getEmitter
      resp = getDatasetChangeProposal[T](dhConfig.fabricType, dhConfig.tags)
      asyncResp <- resp._2
        .map(mcp => ZIO.fromFuture{ implicit ec => Future(emitter.emit(mcp).get()) })
        .reduce(_ *> _)
    } yield (resp._1, asyncResp)


  def getEmitter: ZIO[DatahubConfig, Nothing, Emitter] = for {
    dhConfig <- ZIO.service[DatahubConfig]
  } yield dhConfig.mechanism match {
    case ape.datahub.pipe.EmitterMechanism.REST => RestEmitter.create(dhConfig.getRestEmitterConfig)
    case ape.datahub.pipe.EmitterMechanism.KAFKA => new KafkaEmitter(dhConfig.getKafkaEmitterConfig)
  }
}
