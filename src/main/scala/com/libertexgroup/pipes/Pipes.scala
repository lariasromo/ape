package com.libertexgroup.pipes

import com.libertexgroup.configs._
import com.libertexgroup.pipes
import com.libertexgroup.readers.s3.S3FileReader
import com.libertexgroup.utils.Utils.:=
import zio.Tag

class Pipes() {
  def cassandra[Config <: CassandraConfig :Tag](implicit d: Config := CassandraConfig) = new pipes.cassandra.Pipes[Config]()
  def clickhouse[Config <: MultiClickhouseConfig :Tag](implicit d: Config := MultiClickhouseConfig) = new pipes.clickhouse.Pipes[Config]()
  def jdbc[Config <: JDBCConfig :Tag](implicit d: Config := JDBCConfig) = new pipes.jdbc.Pipes[Config]()
  def kafka[Config <: KafkaConfig :Tag](implicit d: Config := KafkaConfig) = new pipes.kafka.Pipes[Config]()
  def misc = new pipes.misc.Pipes()
  def redis[Config <:RedisConfig :Tag](implicit d: Config := RedisConfig) = new pipes.redis.Pipes[Config]
  def rest = new pipes.rest.Pipes
  def s3[Config <: S3Config :Tag](implicit d: Config := S3Config) = new pipes.s3.fromData.Pipes[Config]
  def s3FileReader[Config <: S3Config :Tag](reader: S3FileReader[Config])(implicit d: Config := S3Config) = new pipes.s3.fromS3Files.Pipes[Config](reader)
}
