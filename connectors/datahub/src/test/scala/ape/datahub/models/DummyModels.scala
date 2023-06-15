package ape.datahub.models

object DummyModels {
  case class Registration(clientId:String)
  case class Country(countryName:String)
  case class RegistrationWithCountry(clientId:String, country:Country)
  case class Person(name:String)
  case class Car(name:String)
  case class House(name:String)
  case class Airplane(name:String)
  case class Building(name:String)
  case class Animal(name:String)
}
