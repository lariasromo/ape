package com.libertexgroup.ape.utils

import com.libertexgroup.configs.KafkaConfig
import com.sksamuel.avro4s.{Encoder, SchemaFor}
import io.circe.syntax.EncoderOps
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord, RecordMetadata}
import zio.ZIO

import java.time.{LocalDateTime, ZoneOffset}
import java.util.Properties
import java.util.concurrent.CompletableFuture
import scala.jdk.javaapi.FutureConverters


object SimpleKafkaProducer {
  val topic = "test_topic"

  private def sendRecordScoped[T](producer: KafkaProducer[String, T], obj: T): ZIO[Any, Throwable, RecordMetadata] = ZIO.scoped {
    val ts = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)
    val record: ProducerRecord[String, T] = new ProducerRecord(topic, 0, ts, "Some key", obj)
    ZIO.acquireRelease{
      ZIO.fromFuture(implicit ec =>
        FutureConverters.asScala(CompletableFuture.supplyAsync(() => producer.send(record).get))
      )
    }{_ => ZIO.succeed(producer.close())}
  }

  def sendRecord(key: String, value:String): ZIO[KafkaConfig, Throwable, RecordMetadata] = ZIO.service[KafkaConfig]
    .flatMap(kafkaConfig => {
      val props:Properties = new Properties()
      props.put("bootstrap.servers",kafkaConfig.kafkaBrokers.mkString(","))
      props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
      props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")
      val producer = new KafkaProducer[String, String](props)

      sendRecordScoped(producer, value)
    })

  def sendRecord(key: String, value:Array[Byte]): ZIO[KafkaConfig, Throwable, RecordMetadata] = ZIO.service[KafkaConfig]
    .flatMap(kafkaConfig => {
      val props:Properties = new Properties()
      props.put("bootstrap.servers",kafkaConfig.kafkaBrokers.mkString(","))
      props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
      props.put("value.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer")
      val producer: KafkaProducer[String, Array[Byte]] = new KafkaProducer[String, Array[Byte]](props)

      sendRecordScoped(producer, value)
    })

  def sendRecord[T: SchemaFor : Encoder](key: String, value:T): ZIO[KafkaConfig, Throwable, RecordMetadata] = ZIO.service[KafkaConfig]
    .flatMap(kafkaConfig => {
      val props:Properties = new Properties()
      props.put("bootstrap.servers",kafkaConfig.kafkaBrokers.mkString(","))
      props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
      props.put("value.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer")
      import AvroUtils.implicits._
      val bytes: Array[Byte] = value.encode().orNull
      val producer = new KafkaProducer[String, Array[Byte]](props)

      sendRecordScoped(producer, bytes)
  })

  def sendRecordCirce[T: SchemaFor : io.circe.Encoder](key: String, value:T): ZIO[KafkaConfig, Throwable, RecordMetadata] =
    ZIO
    .service[KafkaConfig]
    .flatMap(kafkaConfig => {
      val props:Properties = new Properties()
      props.put("bootstrap.servers",kafkaConfig.kafkaBrokers.mkString(","))
      props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
      props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")
      val bytes: String = value.asJson.noSpaces
      val producer = new KafkaProducer[String, String](props)

      sendRecordScoped(producer, bytes)
  })

}
