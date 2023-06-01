package ape.clickhouse.pipes

import ape.clickhouse.configs.{ClickhouseConfig, MultiClickhouseConfig}
import ape.clickhouse.models.ClickhouseModel
import ape.clickhouse.utils.ClickhouseJDBCUtils.runConnect
import com.clickhouse.jdbc.ClickHouseConnection
import zio.Console.printLine
import zio.stream.ZStream
import zio.{Chunk, Tag, ZIO, ZLayer}

import java.sql.Statement
import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}

//protected[writers] class DefaultWriter[E] extends ClickhouseWriter[E, E with Scope with MultiClickhouseConfig, ClickhouseModel] {
// The result of this writer are the records that failed to be inserted
protected[clickhouse] class DefaultPipe[ET, T <:ClickhouseModel :ClassTag, Config <: MultiClickhouseConfig :Tag]
  extends ClickhousePipe[Config, ET, T, Chunk[(T, Int)]] {
  def insertRetrieveResults(batch: Chunk[T]): ClickHouseConnection => Chunk[(T, Int)] = conn => {
    val stmt = conn.prepareStatement(batch.head.sql)
    batch.foreach(row => {
      row.prepare(stmt)
      stmt.addBatch()
    })
    val tryEx: Chunk[Int] = Try(stmt.executeBatch()) match {
      case Failure(exception) => {
        println(exception.getMessage)
        // if the whole batch failed, mark all rows as failed
        batch.map(_ => Statement.EXECUTE_FAILED)
      }
      case Success(value) => Chunk.fromArray(value)
    }

    batch.zip(tryEx)
  }

  def insertBatch(batch: Chunk[T]): ZIO[ClickhouseConfig, Nothing, Chunk[(T, Int)]] = for {
    errors <- runConnect(insertRetrieveResults(batch))
      .catchAll{ error => {
          for {
            _ <- printLine(error.getMessage).catchAll(_=>ZIO.unit)
          } yield batch.map(t => (t, Statement.EXECUTE_FAILED)) // if the whole batch failed, mark all rows as failed
        }
      }
  } yield errors

  def cross[X, Y](xs: Iterable[X], ys: Iterable[Y]): Iterable[(X, Y)] = for {x <- xs; y <- ys} yield (x, y)

  override protected[this] def pipe(i: ZStream[ET, Throwable, T]):
    ZIO[Config, Throwable, ZStream[ET, Throwable, Chunk[(T, Int)]]] = for {
    config <- ZIO.service[Config]
    r = i
      .grouped(config.chConfigs.length * config.chConfigs.head.batchSize * config.parallelism)
      .flatMap{ batch => {
        ZStream.fromIterable {
          {
            val batchSpliced: Chunk[Chunk[T]] = batch.split(config.chConfigs.length * config.parallelism)
            val indexes: Seq[Int] = (1 to config.parallelism)
            val configWithParallelism: List[(ClickhouseConfig, Int)] = cross(config.chConfigs, indexes).toList
            batchSpliced zip configWithParallelism
          }
            .map { case (batch, (config, i)) => ((config,i), batch.length, insertBatch(batch).provideSomeLayer(ZLayer.succeed(config))) }
        }.mapZIOParByKey(_._1) {
          case (((config: ClickhouseConfig, ix: Int), size: Int, eff: ZIO[Any, Nothing, Chunk[(T, Int)]])) =>
            for {
              _ <- printLine(s"Inserting batch on parallel index $ix for CH instance ${config.host} and a batch size of $size")
              eff <- ZIO.scoped(eff)
            } yield eff
        }
      }}
  } yield r
}
