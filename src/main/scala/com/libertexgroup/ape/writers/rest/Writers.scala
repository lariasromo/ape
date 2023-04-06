package com.libertexgroup.ape.writers.rest

class Writers() {
  def byte[ZE] = new RestAPIWriterByte[ZE]
  def string[ZE] = new RestAPIWriterString[ZE]
}
