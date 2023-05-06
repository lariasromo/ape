package com.libertexgroup.pipes

import com.libertexgroup.configs._
import com.libertexgroup.readers.s3.S3FileReader
import com.libertexgroup.pipes
import com.libertexgroup.pipes.redis.Pipes
import zio.Tag

class Pipes() {
  def cassandra[Config <: CassandraConfig :Tag] = new pipes.cassandra.Pipes[Config]()
  def clickhouse[Config <: MultiClickhouseConfig :Tag] = new pipes.clickhouse.Pipes[Config]()
  def jdbc[Config <: JDBCConfig :Tag] = new pipes.jdbc.Pipes[Config]()
  def kafka[Config <: KafkaConfig :Tag] = new pipes.kafka.Pipes[Config]()
  def misc = new pipes.misc.Pipes()
  def redis[Config <:RedisConfig :Tag] = new pipes.redis.Pipes[Config]
  def rest = new pipes.rest.Pipes
  def s3[Config <: S3Config :Tag] = new pipes.s3.fromData.Pipes[Config]
  def s3FileReader[Config <: S3Config :Tag](reader: S3FileReader[Config]) = new pipes.s3.fromS3Files.Pipes[Config](reader)
}
