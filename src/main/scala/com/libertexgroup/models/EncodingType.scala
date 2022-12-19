package com.libertexgroup.models

object EncodingType extends Enumeration {
  type EncodingType = Value
//  GZIP and GUNZIP are the same
  val PARQUET, GZIP, GUNZIP, PLAINTEXT, AVRO = Value
}
