package com.libertexgroup.readers

import com.libertexgroup.configs._
import com.libertexgroup.readers
import zio.Tag

// Readers
class PipelineReaders() {
  def cassandra[Config <: CassandraConfig :Tag] = new readers.cassandra.Readers[Config]()
  def clickhouse[Config <: MultiClickhouseConfig :Tag] = new readers.clickhouse.Readers[Config]()
  def jdbc[Config <: JDBCConfig :Tag] = new readers.jdbc.Readers[Config]()
  def kafka[Config <: KafkaConfig :Tag] = new readers.kafka.Readers[Config]()
  def redis[Config <: RedisConfig :Tag] = new readers.redis.Readers[Config]()
  def rest = new readers.rest.Readers()
  def s3[Config <: S3Config :Tag] = new readers.s3.Readers[Config]()
  def s3FileReaders[Config <: S3Config :Tag] = new readers.s3.S3FileReaders[Config]()
  def websocket = new readers.websocket.Readers()
}
