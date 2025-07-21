package gr.gm.industry.utils.enums

import reactivemongo.api.bson.{BSONReader, BSONString, BSONWriter}

sealed trait Currency {
  val name = ""
}

object Currency {

  case object EUR extends Currency {
    override val name = "EUR"
  }

  case object USD extends Currency {
    override val name = "USD"
  }

  case object USDT extends Currency {
    override val name = "USDT"
  }
  val all: List[Currency] = List(EUR, USD)

  private val byName: Map[String, Currency] = all.map(c => c.name -> c).toMap

  def get(name: String): Option[Currency] = byName.get(name)

  implicit val currencyWriter: BSONWriter[Currency] = BSONWriter { currency =>
    BSONString(currency.name)
  }

  implicit val currencyReader: BSONReader[Currency] = BSONReader {
    case BSONString(name) =>
      Currency.get(name).getOrElse {
        throw new Exception(s"Unknown currency name: $name")
      }
    case _ =>
      throw new Exception("Expected BSONString when reading currency")
  }
}
