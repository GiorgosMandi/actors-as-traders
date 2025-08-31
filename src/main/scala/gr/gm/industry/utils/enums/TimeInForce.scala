package gr.gm.industry.utils.enums

sealed trait TimeInForce {
  val name = ""
  val description = ""
}
object TimeInForce {

  case object GTC extends TimeInForce {
    override val name = "GTC"
    override val description: String = "Good Till Cancelled"
  }

  case object IOC extends TimeInForce {
    override val name = "IOC"
    override val description: String = "Immediate Or Cancel"
  }

  case object FOK extends TimeInForce {
    override val name = "FOK"
    override val description: String = "Fill Or Kill"
  }

}
