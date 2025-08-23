package gr.gm.industry.strategies

import gr.gm.industry.dto.BookTickerPriceDto
import gr.gm.industry.model.TradeDecision
import gr.gm.industry.model.TradeDecision.{NoAction, OrderIntent}
import gr.gm.industry.utils.enums.Side.{BUY, SELL}

import scala.util.Random

object RandomStrategy extends Strategy {

  private val rng = new Random(System.currentTimeMillis)

  def decide(price: BookTickerPriceDto): TradeDecision = {
    val quantity = rng.nextInt(100) + 1
    rng.nextInt(6) match {
      case 1 => OrderIntent(price.bestBidPrice, quantity, BUY, price.symbol)
      case 2 => OrderIntent(price.bestBidPrice, quantity, SELL, price.symbol)
      case _ => NoAction
    }
  }
}
