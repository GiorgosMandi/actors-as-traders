package gr.gm.industry.utils

object Constants {

  case object ACK

  sealed trait Coin {
    val name = ""
  }

  case object ADA extends Coin {
    override val name = "ADA"
  }

  case object ETH extends Coin {
    override val name = "ETH"
  }

  case object BTC extends Coin {
    override val name = "ADA"
  }

  object Coin {
    val options: Map[String, Coin] = List(ADA, ETH, BTC).map(c => c.name -> c).toMap

    def get(name: String): Option[Coin] = options.get(name)
  }

  sealed trait Currency {
    val name = ""
  }

  case object EUR extends Currency {
    override val name = "EUR"
  }

  case object DOL extends Currency {
    override val name = "DOL"
  }

  object Currency {
    val options: Map[String, Currency] = List(EUR, DOL).map(c => c.name -> c).toMap

    def get(name: String): Option[Currency] = options.get(name)
  }

}

