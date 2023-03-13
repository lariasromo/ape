package com.libertexgroup.configs

case class CassandraConfig(
                            host: String,
                            keyspace: String,
                            port: Int,
                            batchSize: Int,
                            syncDuration: zio.Duration,
                            username: String,
                            password: String,
                            datacenter: String="datacenter1",
                          )