package gr.gm.industry.model

import gr.gm.industry.utils.enums.Side
import gr.gm.industry.utils.model.TradingSymbol

/** Decision outcome produced by a trading strategy. */
sealed trait TradeDecision

object TradeDecision {

  /**
   * Expresses intent to place an order with price, quantity, side, and symbol.
   */
  case class OrderIntent(
                          price: BigDecimal,
                          quantity: BigDecimal,
                          side: Side,
                          symbol: TradingSymbol
                        ) extends TradeDecision

  /** Indicates no trade should be placed for the current signal. */
  case object NoAction extends TradeDecision
}
