package gr.gm.industry.utils.enums

sealed trait Side

object Side {
  case object BUY extends Side

  case object SELL extends Side

  case object UNKNOWN extends Side

  def apply(s: String): Side =
    s.toUpperCase() match {
      case "BUY" => BUY
      case "SELL" => SELL
      case _ => UNKNOWN
    }
}

