package gr.gm.industry.utils.enums

sealed trait OrderStatus {
  val name = ""
}

object OrderStatus {

  case object NEW extends OrderStatus {
    override val name = "NEW"
  }

  case object PARTIALLY_FILLED extends OrderStatus {
    override val name = "PARTIALLY_FILLED"
  }

  case object FILLED extends OrderStatus {
    override val name = "FILLED"
  }

  case object CANCELED extends OrderStatus {
    override val name = "CANCELED"
  }
}
