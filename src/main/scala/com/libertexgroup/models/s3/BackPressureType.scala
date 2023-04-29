package com.libertexgroup.models.s3

object BackPressureType extends Enumeration {
  type BackPressureType = Value
  val ZIO, REDIS, NONE = Value
}
