package ape.s3.models

object CompressionType extends Enumeration {
  type CompressionType = Value
  //  GZIP and GUNZIP are the same
  val GZIP, GUNZIP, NONE = Value
}
