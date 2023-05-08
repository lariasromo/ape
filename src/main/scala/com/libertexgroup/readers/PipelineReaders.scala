package com.libertexgroup.readers

import com.libertexgroup.configs._
import com.libertexgroup.readers
import com.libertexgroup.utils.Utils.:=
import zio.Tag

// Readers
class PipelineReaders() {
  def cassandra[Config <: CassandraConfig :Tag](implicit d1: Config := CassandraConfig) =
    new readers.cassandra.Readers[Config]()
  def clickhouse[Config <: MultiClickhouseConfig :Tag](implicit d1: Config := MultiClickhouseConfig) =
    new readers.clickhouse.Readers[Config]()
  def jdbc[Config <: JDBCConfig :Tag](implicit d1: Config := JDBCConfig) =
    new readers.jdbc.Readers[Config]()
  def kafka[Config <: KafkaConfig :Tag](implicit d1: Config := KafkaConfig) =
    new readers.kafka.Readers[Config]()
  def redis[Config <: RedisConfig :Tag](implicit d1: Config := RedisConfig) =
    new readers.redis.Readers[Config]()
  def rest =
    new readers.rest.Readers()
  def s3[Config <: S3Config :Tag](implicit d1: Config := S3Config) =
    new readers.s3.Readers[Config]()
  def s3FileReaders[Config <: S3Config :Tag](implicit d1: Config := S3Config) =
    new readers.s3.S3FileReaders[Config]()
  def websocket =
    new readers.websocket.Readers()
}
