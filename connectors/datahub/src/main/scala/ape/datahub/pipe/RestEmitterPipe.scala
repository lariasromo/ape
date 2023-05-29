package ape.datahub.pipe

import ape.datahub.DatahubDataset
import ape.datahub.configs.DatahubConfig
import ape.datahub.utils.DatahubUtils
import ape.pipe.Pipe
import zio.Console.printLine
import zio.ZIO
import zio.stream.ZStream

import scala.reflect.ClassTag

class RestEmitterPipe[E, ZE, T1 : DatahubDataset :ClassTag, T2 : DatahubDataset :ClassTag](
      p: Pipe[E, ZE, T1, T2],
      emitterType: EmitterType.Value
  ) extends Pipe[E with DatahubConfig, ZE, T1, T2] {
  override protected[this] def pipe(i: ZStream[ZE, Throwable, T1]):
    ZIO[E with DatahubConfig, Throwable, ZStream[ZE, Throwable, T2]] =
      for {
        _ <- ZIO.when(Seq(EmitterType.BOTH, EmitterType.LEFT) contains emitterType) {
          for {
            _ <- printLine("Dataset Name: " + implicitly[DatahubDataset[T1]].datasetName)
            _ <- printLine("Dataset Description: " + implicitly[DatahubDataset[T1]].datasetDescription)
            _ <- DatahubUtils.emitRest[T2]
          } yield ()
        }
        s <- p.apply(i)
        _ <- ZIO.when(Seq(EmitterType.BOTH, EmitterType.RIGHT) contains emitterType) {
          for {
            _ <- printLine("Dataset Name: " + implicitly[DatahubDataset[T2]].datasetName)
            _ <- printLine("Dataset Description: " + implicitly[DatahubDataset[T2]].datasetDescription)
            _ <- DatahubUtils.emitRest[T2]
          } yield ()
        }
      } yield s

  override def name: String = p.name
}

object RestEmitterPipe {
  def fromPipe[E, ZE,  T1 :DatahubDataset :ClassTag, T2 :DatahubDataset :ClassTag](pipe: Pipe[E, ZE, T1, T2] ) =
    new RestEmitterPipe[E, ZE, T1, T2](pipe, EmitterType.RIGHT)

  def fromPipeBoth[E, ZE,  T1 :DatahubDataset :ClassTag, T2 :DatahubDataset :ClassTag](pipe: Pipe[E, ZE, T1, T2]) =
    new RestEmitterPipe[E, ZE, T1, T2](pipe, EmitterType.RIGHT)

  def fromPipeRight[E, ZE,  T1 :DatahubDataset :ClassTag, T2 :DatahubDataset :ClassTag](pipe: Pipe[E, ZE, T1, T2]) =
    new RestEmitterPipe[E, ZE, T1, T2](pipe, EmitterType.BOTH)

  def fromPipeLeft[E, ZE,  T1 :DatahubDataset :ClassTag, T2 :DatahubDataset :ClassTag](pipe: Pipe[E, ZE, T1, T2]) =
    new RestEmitterPipe[E, ZE, T1, T2](pipe, EmitterType.LEFT)

  def apply[E, ZE, T1 :DatahubDataset :ClassTag, T2 :DatahubDataset :ClassTag](pipe: Pipe[E, ZE, T1, T2]) =
    fromPipe(pipe)
}
