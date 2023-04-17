package com.libertexgroup.ape.writers.cassandra

import com.dimafeng.testcontainers.CassandraContainer
import com.libertexgroup.ape.{Ape, Reader}
import com.libertexgroup.ape.models.dummy
import com.libertexgroup.ape.utils.CassandraContainerService
import com.libertexgroup.ape.writers.sampleData
import com.libertexgroup.configs.CassandraConfig
import zio.test.{Spec, TestEnvironment, ZIOSpec, assertTrue}
import zio.{Scope, ZLayer}

object CassandraWriterTest  extends ZIOSpec[CassandraConfig with CassandraContainer] {
  val writer = Ape.writers.cassandra[CassandraConfig].default[Any, dummy]

  override def spec: Spec[CassandraConfig with CassandraContainer with TestEnvironment with Scope, Any] =
    suite("CassandraWriterTest")(
      test("Writes dummy data"){
        for {
          _ <- writer.write(sampleData)
        } yield {
          assertTrue(true)
        }
      }
    )

  override def bootstrap: ZLayer[Any, Any, CassandraConfig with CassandraContainer] =
    CassandraContainerService.layer >+> ZLayer.fromZIO(CassandraContainerService.createTable)
}
