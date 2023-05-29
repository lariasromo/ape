package ape.datahub.models

import ape.datahub.DatahubDataset
import ape.datahub.DatahubDataset.{dataHubDesc, withDatahubDescription}

object DummyModels {
  case class Person(name:String)
  object Person extends withDatahubDescription[Person]("Person", "This is a Person")
  case class Car(name:String)
  object Car extends withDatahubDescription[Car]("Car", "This is a Car")
  case class House(name:String)
  object House extends withDatahubDescription[House]("House", "This is a House")
  case class Airplane(name:String)
  object Airplane extends withDatahubDescription[Airplane]("Airplane", "This is an Airplane")
  case class Building(name:String)
  object Building extends withDatahubDescription[Building]("Building", "This is a Building")
  case class Animal(name:String)
  object Animal extends withDatahubDescription[Animal]("Animal", "This is an Animal")
  implicit val stringDH: DatahubDataset[String] = dataHubDesc[String]("String", "Some string")
}
