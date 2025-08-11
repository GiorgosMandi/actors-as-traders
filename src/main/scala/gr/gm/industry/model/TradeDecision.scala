package gr.gm.industry.model

import gr.gm.industry.utils.enums.Side
import gr.gm.industry.utils.model.TradingSymbol

sealed trait TradeDecision

object TradeDecision {

  case class OrderIntent(
                          price: BigDecimal,
                          quantity: BigDecimal,
                          side: Side,
                          symbol: TradingSymbol
                        ) extends TradeDecision
  case object NoAction extends TradeDecision
}

