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

  case object REJECTED extends OrderStatus {
    override val name = "REJECTED"
  }

  case object EXPIRED extends OrderStatus {
    override val name = "EXPIRED"
  }


  case object UNKNOWN extends OrderStatus {
    override val name = "UNKNOWN"
  }

  case object FAILED extends OrderStatus {
    override val name = "FAILED"
  }


  def apply(s: String): OrderStatus =
    s.toUpperCase() match {
      case "NEW" => NEW
      case "PARTIALLY_FILLED" => PARTIALLY_FILLED
      case "FILLED" => FILLED
      case "CANCELED" => CANCELED
      case "FAILED" => FAILED
      case _ => UNKNOWN

    }
}
