package gr.gm.industry.utils.enums

sealed trait Side
object Side {
  case object BUY extends Side
  case object SELL extends Side
}

