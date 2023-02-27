package com.libertexgroup.ape.utils

import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy
import org.testcontainers.utility.Base58

import java.net.URI
import java.time.Duration

object MinioContainer {
  private val ADDRESS_PORT = 9001
  private val CONSOLE_ADDRESS_PORT = 9000
  private val DEFAULT_IMAGE = "quay.io/minio/minio"
  private val MINIO_ACCESS_KEY = "MINIO_ACCESS_KEY"
  private val MINIO_SECRET_KEY = "MINIO_SECRET_KEY"
  private val DEFAULT_STORAGE_DIRECTORY = "/data"
  private val HEALTH_ENDPOINT = "/minio/health/live"
  private val accessKey = "minio"
  private val secretKey = "minio123"

  class MinioContainer()
    extends GenericContainer[MinioContainer](DEFAULT_IMAGE) {
      withNetworkAliases("minio-" + Base58.randomString(6))
      addExposedPort(ADDRESS_PORT)
      addExposedPort(CONSOLE_ADDRESS_PORT)

      withEnv(MINIO_ACCESS_KEY, accessKey)
      withEnv(MINIO_SECRET_KEY, secretKey)

      //withCommand("server", MinioContainer.DEFAULT_STORAGE_DIRECTORY)
      withCommand("server",
        "--address", s":${ADDRESS_PORT}",
        "--console-address", s":${CONSOLE_ADDRESS_PORT}",
        MinioContainer.DEFAULT_STORAGE_DIRECTORY
      )

      setWaitStrategy(
        new HttpWaitStrategy()
          .forPort(MinioContainer.ADDRESS_PORT)
          .forPath(MinioContainer.HEALTH_ENDPOINT)
          .withStartupTimeout(Duration.ofMinutes(2))
      )

      def getHostAddress: URI = URI.create("http://" + getHost + ":" + getMappedPort(ADDRESS_PORT))
      def getAwsAccessKey: String = accessKey
      def getAwsSecretKey: String = secretKey
  }
}