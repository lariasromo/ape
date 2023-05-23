package ape.models

case class dummy(a: String, b: String) {
  override def toString: String = s"$a, $b"
}