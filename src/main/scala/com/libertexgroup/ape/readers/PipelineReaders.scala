package com.libertexgroup.ape.readers

import com.libertexgroup.configs._
import zio.Tag

// Readers
class PipelineReaders() {
  def cassandra[Config <: CassandraConfig :Tag] = new com.libertexgroup.ape.readers.cassandra.Readers[Config]()
  def clickhouse[Config <: MultiClickhouseConfig :Tag] = new com.libertexgroup.ape.readers.clickhouse.Readers[Config]()
  def jdbc[Config <: JDBCConfig :Tag] = new com.libertexgroup.ape.readers.jdbc.Readers[Config]()
  def kafka[Config <: KafkaConfig :Tag] = new com.libertexgroup.ape.readers.kafka.Readers[Config]()
  def redis[Config <: RedisConfig :Tag] = new com.libertexgroup.ape.readers.redis.Readers[Config]()
  def rest = new com.libertexgroup.ape.readers.rest.Readers()
  def s3[Config <: S3Config :Tag] = new com.libertexgroup.ape.readers.s3.Readers[Config]()
  def s3FileReaders[Config <: S3Config :Tag] = new com.libertexgroup.ape.readers.s3.S3FileReaders[Config]()
  def websocket = new com.libertexgroup.ape.readers.websocket.Readers()
}
