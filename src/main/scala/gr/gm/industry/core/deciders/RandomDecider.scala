package gr.gm.industry.core.deciders

import gr.gm.industry.model.TradeDecision.{NoAction, OrderIntent}
import gr.gm.industry.model.{CoinPrice, TradeDecision}
import gr.gm.industry.utils.enums.Side.{BUY, SELL}

import scala.util.Random

object RandomDecider extends DecisionMaker {

  def decide(price: CoinPrice): TradeDecision = {
    val quantity = Random.nextInt(100)
    new Random(System.currentTimeMillis).nextInt(6) match {
      case 1 => OrderIntent(price.price, quantity, BUY, price.symbol)
      case 2 => OrderIntent(price.price, quantity, SELL, price.symbol)
      case _ => NoAction
    }
  }
}
