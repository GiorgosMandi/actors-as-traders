package gr.gm.industry.utils

import gr.gm.industry.core.traders.NaivePendingTrader.TradeActionEvent


object TradeActions {

  sealed trait TradeAction

  sealed trait OrderType extends TradeAction {
    val name = ""
  }

  case object BUY extends OrderType {
    override val name = "BUY"
  }

  case object SELL extends OrderType {
    override val name = "SELL"
  }

  case object OMIT extends TradeAction

  case object PRINT extends TradeAction

  def elect(tradeActions: Product): TradeActionEvent = {
    val tradeActionsList = tradeActions.productIterator
      .map(_.asInstanceOf[TradeActionEvent])
      .toList
    val votes = tradeActionsList
      .foldLeft(Map[TradeActionEvent, Int]().withDefaultValue(0)) {
        case (acc, letter) => acc + (letter -> (1 + acc(letter)))
      }
    votes.maxBy(_._2)._1
  }
}


