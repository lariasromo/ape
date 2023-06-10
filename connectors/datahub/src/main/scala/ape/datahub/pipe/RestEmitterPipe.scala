package ape.datahub.pipe

import ape.datahub.configs.DatahubConfig
import ape.datahub.utils.DatahubUtils
import ape.pipe.Pipe
import com.sksamuel.avro4s.SchemaFor
import zio.ZIO
import zio.stream.ZStream

import scala.reflect.{ClassTag, classTag}

class RestEmitterPipe[E, ZE, T1 : SchemaFor :ClassTag, T2 : SchemaFor :ClassTag](
      p: Pipe[E, ZE, T1, T2],
      emitterType: EmitterType.Value
  ) extends Pipe[E with DatahubConfig, ZE, T1, T2] {
  override protected[this] def pipe(i: ZStream[ZE, Throwable, T1]):
    ZIO[E with DatahubConfig, Throwable, ZStream[ZE, Throwable, T2]] =
      for {
        _ <- ZIO.when(Seq(EmitterType.BOTH, EmitterType.LEFT) contains emitterType) {
          for {
            _ <-  ZIO.logInfo("Dataset Name: " + classTag[T2].runtimeClass.getSimpleName)
            emitResp <- DatahubUtils.emitRest[T2]
            _ <- if(emitResp.isSuccess){
              ZIO.logInfo("Successfully emitted metadata event") *>
                ZIO.logInfo(emitResp.toString) *>
                ZIO.logInfo(emitResp.getResponseContent)
            } else {
              ZIO.logInfo("Failed to emit metadata event") *>
                ZIO.logInfo(emitResp.getUnderlyingResponse.toString)
            }
          } yield ()
        }
        s <- p.apply(i)
        _ <- ZIO.when(Seq(EmitterType.BOTH, EmitterType.RIGHT) contains emitterType) {
          for {
            _ <- ZIO.logInfo("Dataset Name: " + classTag[T2].runtimeClass.getSimpleName)
            emitResp <- DatahubUtils.emitRest[T2]
            _ <- if(emitResp.isSuccess){
              ZIO.logInfo("Successfully emitted metadata event") *>
                ZIO.logInfo(emitResp.toString) *>
                ZIO.logInfo(emitResp.getResponseContent)
            } else {
              ZIO.logInfo("Failed to emit metadata event") *>
                ZIO.logInfo(emitResp.getUnderlyingResponse.toString)
            }
          } yield ()
        }
      } yield s

  override def name: String = p.name
}

object RestEmitterPipe {
  def fromPipe[E, ZE,  T1 :SchemaFor :ClassTag, T2 :SchemaFor :ClassTag](pipe: Pipe[E, ZE, T1, T2] ) =
    new RestEmitterPipe[E, ZE, T1, T2](pipe, EmitterType.RIGHT)

  def fromPipeBoth[E, ZE,  T1 :SchemaFor :ClassTag, T2 :SchemaFor :ClassTag](pipe: Pipe[E, ZE, T1, T2]) =
    new RestEmitterPipe[E, ZE, T1, T2](pipe, EmitterType.RIGHT)

  def fromPipeRight[E, ZE,  T1 :SchemaFor :ClassTag, T2 :SchemaFor :ClassTag](pipe: Pipe[E, ZE, T1, T2]) =
    new RestEmitterPipe[E, ZE, T1, T2](pipe, EmitterType.BOTH)

  def fromPipeLeft[E, ZE,  T1 :SchemaFor :ClassTag, T2 :SchemaFor :ClassTag](pipe: Pipe[E, ZE, T1, T2]) =
    new RestEmitterPipe[E, ZE, T1, T2](pipe, EmitterType.LEFT)

  def apply[E, ZE, T1 :SchemaFor :ClassTag, T2 :SchemaFor :ClassTag](pipe: Pipe[E, ZE, T1, T2]) =
    fromPipe(pipe)
}
