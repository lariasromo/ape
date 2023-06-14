package ape.datahub.pipe

import ape.datahub.configs.DatahubConfig
import ape.datahub.utils.DatahubUtils
import ape.pipe.Pipe
import com.sksamuel.avro4s.SchemaFor
import zio.ZIO
import zio.stream.ZStream

import scala.reflect.{ClassTag, classTag}

class EmitterPipe[-E, ZE, T1 : SchemaFor :ClassTag, T2 : SchemaFor :ClassTag](
      p: Pipe[E, ZE, T1, T2],
      emitterType: EmitterType.Value
  ) extends Pipe[E with DatahubConfig, ZE, T1, T2] {

  def createDatasets: ZIO[DatahubConfig, Throwable, Unit] = for {
    urnLeft <- ZIO.when(Seq(EmitterType.BOTH, EmitterType.LEFT) contains emitterType) {
      for {
        _ <-  ZIO.logInfo("Dataset Name: " + classTag[T1].runtimeClass.getSimpleName)
        resp <- DatahubUtils.emitDataset[T1]
        (urn, emitResp) = resp
        _ <- if(emitResp.isSuccess){
          ZIO.logInfo("Successfully emitted metadata event") *>
            ZIO.logInfo(emitResp.toString) *>
            ZIO.logInfo(emitResp.getResponseContent)
        } else {
          ZIO.logInfo("Failed to emit metadata event") *>
            ZIO.logInfo(emitResp.getUnderlyingResponse.toString)
        }
      } yield urn
    }
    urnRight <- ZIO.when(Seq(EmitterType.BOTH, EmitterType.RIGHT) contains emitterType) {
      for {
        _ <- ZIO.logInfo("Dataset Name: " + classTag[T2].runtimeClass.getSimpleName)
        resp <- DatahubUtils.emitDataset[T2]
        (urn, emitResp) = resp
        _ <- if(emitResp.isSuccess){
          ZIO.logInfo("Successfully emitted metadata event") *>
            ZIO.logInfo(emitResp.toString) *>
            ZIO.logInfo(emitResp.getResponseContent)
        } else {
          ZIO.logInfo("Failed to emit metadata event") *>
            ZIO.logInfo(emitResp.getUnderlyingResponse.toString)
        }
      } yield urn
    }
    lineageResp <- ZIO.when(emitterType.equals(EmitterType.BOTH) && urnLeft.isDefined && urnRight.isDefined) {
      for {
        lineageResp <- DatahubUtils.emitLineage(Seq(urnLeft.orNull), urnRight.orNull)
        _ <- if(lineageResp.isSuccess){
          ZIO.logInfo("Successfully emitted metadata lineage event") *>
            ZIO.logInfo(lineageResp.toString) *>
            ZIO.logInfo(lineageResp.getResponseContent)
        } else {
          ZIO.logInfo("Failed to emit metadata lineage event") *>
            ZIO.logInfo(lineageResp.getUnderlyingResponse.toString)
        }
      } yield lineageResp
    }
  } yield ()

  override protected[this] def pipe(i: ZStream[ZE, Throwable, T1]):
    ZIO[E with DatahubConfig, Throwable, ZStream[ZE, Throwable, T2]] =
      for {
        _ <- createDatasets
        s <- p.apply(i)
      } yield s

  override def name: String = p.name
}

object EmitterPipe {
  def fromPipeBoth[E, ZE,  T1 :SchemaFor :ClassTag, T2 :SchemaFor :ClassTag](pipe: Pipe[E, ZE, T1, T2]) =
    new EmitterPipe[E, ZE, T1, T2](pipe, EmitterType.BOTH)

  def fromPipeRight[E, ZE,  T1 :SchemaFor :ClassTag, T2 :SchemaFor :ClassTag](pipe: Pipe[E, ZE, T1, T2]) =
    new EmitterPipe[E, ZE, T1, T2](pipe, EmitterType.RIGHT)

  def fromPipeLeft[E, ZE,  T1 :SchemaFor :ClassTag, T2 :SchemaFor :ClassTag](pipe: Pipe[E, ZE, T1, T2]) =
    new EmitterPipe[E, ZE, T1, T2](pipe, EmitterType.LEFT)

  def apply[E, ZE, T1 :SchemaFor :ClassTag, T2 :SchemaFor :ClassTag](pipe: Pipe[E, ZE, T1, T2]) =
    fromPipeBoth(pipe)
}
