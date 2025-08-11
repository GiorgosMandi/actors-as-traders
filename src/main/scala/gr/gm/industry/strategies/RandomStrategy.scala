package gr.gm.industry.strategies

import gr.gm.industry.model.TradeDecision.{NoAction, OrderIntent}
import gr.gm.industry.model.{CoinPrice, TradeDecision}
import gr.gm.industry.utils.enums.Side.{BUY, SELL}

import scala.util.Random

object RandomStrategy extends Strategy {

  private val rng = new Random(System.currentTimeMillis)

  def decide(price: CoinPrice): TradeDecision = {
    val quantity = rng.nextInt(100) + 1
    rng.nextInt(6) match {
      case 1 => OrderIntent(price.price, quantity, BUY, price.symbol)
      case 2 => OrderIntent(price.price, quantity, SELL, price.symbol)
      case _ => NoAction
    }
  }
}
