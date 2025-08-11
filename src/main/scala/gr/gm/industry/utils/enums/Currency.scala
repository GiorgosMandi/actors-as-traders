package gr.gm.industry.utils.enums

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

  val all: List[Currency] = List(EUR, USD, USDT)

  private val byName: Map[String, Currency] = all.map(c => c.name -> c).toMap

  def get(name: String): Option[Currency] = byName.get(name)
}
