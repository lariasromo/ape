package ape.datahub

trait DatahubDataset[+T] {
  lazy val datasetName: String = ???
  lazy val datasetDescription: String = ???
}

object DatahubDataset {
  def dataHubDesc[T](name: String, description: String): DatahubDataset[T] = new DatahubDataset[T] {
    override lazy val datasetName: String = name
    override lazy val datasetDescription: String = description
  }

  class withDatahubDescription[+T](name: String, description: String) {
    implicit lazy val i: DatahubDataset[T] = dataHubDesc[T](name, description)
  }
}

