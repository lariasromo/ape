package ape.datahub.pipe

object EmitterMechanism extends Enumeration {
  type EmitterMechanism = Value
  val REST, KAFKA = Value
}