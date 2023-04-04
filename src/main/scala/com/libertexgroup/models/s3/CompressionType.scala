package com.libertexgroup.models.s3

object CompressionType extends Enumeration {
  type CompressionType = Value
  //  GZIP and GUNZIP are the same
  val GZIP, GUNZIP, NONE = Value
}
