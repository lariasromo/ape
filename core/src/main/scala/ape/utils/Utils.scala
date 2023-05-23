package ape.utils

import io.circe.{Decoder, Encoder}
import zio.{Tag, ULayer, ZIO, ZLayer}
import scala.language.experimental.macros

import scala.reflect.macros.whitebox

object Utils {
  def reLayer[T: Tag]: ZIO[T, Nothing, ULayer[T]] = for {
    t <- ZIO.service[T]
  } yield ZLayer.succeed(t)

  class :=[T, Q]

  trait Default_:= {
    /** Ignore default */
    implicit def useProvided[Provided, Default] = new :=[Provided, Default]
  }

  object := extends Default_:= {
    /** Infer type argument to default */
    implicit def useDefault[Default] = new :=[Default, Default]
  }

  trait SerializableTrait[A] {
    implicit def someEncoder: Encoder[A] = macro SerializableTraitMacros.someEncoderImpl[A]

    implicit def someDecoder: Decoder[A] = macro SerializableTraitMacros.someDecoderImpl[A]
  }

  class SerializableTraitMacros(val c: whitebox.Context) {

    import c.universe._

    val semiauto = q"_root_.io.circe.generic.semiauto"

    def someEncoderImpl[A: WeakTypeTag]: Tree = q"$semiauto.deriveEncoder[${weakTypeOf[A]}]"

    def someDecoderImpl[A: WeakTypeTag]: Tree = q"$semiauto.deriveDecoder[${weakTypeOf[A]}]"
  }
}
