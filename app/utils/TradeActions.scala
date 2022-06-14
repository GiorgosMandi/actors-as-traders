package utils
sealed trait TradeActions

object TradeActions {
  case object BUY extends TradeActions
  case object SELL extends TradeActions
  case object OMIT extends TradeActions
}
