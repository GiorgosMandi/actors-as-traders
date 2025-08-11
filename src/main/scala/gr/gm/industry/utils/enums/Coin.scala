package gr.gm.industry.utils.enums


sealed trait Coin {
  val name = ""
}

object Coin {

  case object ADA extends Coin {
    override val name = "ADA"
  }

  case object ETH extends Coin {
    override val name = "ETH"
  }

  case object BTC extends Coin {
    override val name = "BTC"
  }

  val all: List[Coin] = List(ADA, ETH, BTC)

  private val byName: Map[String, Coin] = all.map(c => c.name -> c).toMap

  def get(name: String): Option[Coin] = byName.get(name)
}