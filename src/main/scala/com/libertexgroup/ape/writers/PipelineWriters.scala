package com.libertexgroup.ape.writers

import com.libertexgroup.configs._
import zio.Tag

class PipelineWriters() {
  def cassandra[Config <: CassandraConfig :Tag] = new com.libertexgroup.ape.writers.cassandra.Writers[Config]()
  def clickhouse[Config <: MultiClickhouseConfig :Tag] = new com.libertexgroup.ape.writers.clickhouse.Writers[Config]()
  def jdbc[Config <: JDBCConfig :Tag] = new com.libertexgroup.ape.writers.jdbc.Writers[Config]()
  def kafka[Config <: KafkaConfig :Tag] = new com.libertexgroup.ape.writers.kafka.Writers[Config]()
  def misc = new com.libertexgroup.ape.writers.misc.Writers()
  def redis[Config <:RedisConfig :Tag] = new com.libertexgroup.ape.writers.redis.Writers.Writers[Config]()
  def rest = new com.libertexgroup.ape.writers.rest.Writers
  def s3[Config <: S3Config :Tag] = new com.libertexgroup.ape.writers.s3.Writers[Config]
}
