package ape.datahub.utils

import ape.datahub.configs.DatahubConfig
import com.linkedin.common.urn.{DataPlatformUrn, DatasetUrn, TagUrn, Urn}
import com.linkedin.common.{AuditStamp, FabricType, GlobalTags, TagAssociation, TagAssociationArray}
import com.linkedin.schema.SchemaFieldDataType.Type
import com.linkedin.schema.SchemaMetadata.PlatformSchema
import com.linkedin.schema._
import com.sksamuel.avro4s.{AvroSchema, SchemaFor}
import datahub.client.MetadataWriteResponse
import datahub.client.kafka.KafkaEmitter
import datahub.client.rest.RestEmitter
import datahub.event.MetadataChangeProposalWrapper
import org.apache.avro.Schema
import org.apache.avro.Schema.Type._
import zio.ZIO

import java.time.{LocalDateTime, ZoneOffset}
import scala.concurrent.Future
import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.reflect.{ClassTag, classTag}

object DatahubUtils {

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

  def getMCPs[T: ClassTag :SchemaFor](fabricType: FabricType = FabricType.DEV, tags:Seq[String] = Seq.empty) = {
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
    Seq(c1, c2)
  }

  def emitRest[T: ClassTag :SchemaFor]: ZIO[DatahubConfig, Throwable, MetadataWriteResponse] = for {
    dhConfig <- ZIO.service[DatahubConfig]
    f <- {
      val emitter = RestEmitter.create(dhConfig.getRestEmitterConfig)
      getMCPs[T](dhConfig.fabricType, dhConfig.tags)
        .map(mcp => ZIO.fromFuture{ implicit ec => Future(emitter.emit(mcp).get()) })
        .reduce(_ *> _)
    }
  } yield f

  def emitKafka[T: ClassTag :SchemaFor]: ZIO[DatahubConfig, Throwable, MetadataWriteResponse] = for {
    dhConfig <- ZIO.service[DatahubConfig]
    f <- {
      val emitter = new KafkaEmitter(dhConfig.getKafkaEmitterConfig)
      getMCPs[T](dhConfig.fabricType, dhConfig.tags)
        .map(mcp => ZIO.fromFuture{ implicit ec => Future(emitter.emit(mcp).get()) })
        .reduce(_ *> _)
    }
  } yield f
}
