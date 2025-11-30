package gr.gm.industry.model.orders

import gr.gm.industry.model.TradeDecision.OrderIntent
import gr.gm.industry.utils.enums.{OrderStatus, Side}
import gr.gm.industry.utils.model.TradingSymbol

import java.time.Instant

/**
 * Represents an order intent that failed during placement.
 *
 * @param reason    error description from the exchange or client.
 * @param price     intended price.
 * @param quantity  intended quantity.
 * @param side      BUY/SELL side.
 * @param symbol    trading pair.
 * @param status    status at failure (default NEW).
 * @param placedAt  timestamp when the failure was recorded.
 */
case class FailedPlacedOrder(reason: String,
                             price: BigDecimal,
                             quantity: BigDecimal,
                             side: Side,
                             symbol: TradingSymbol,
                             status: OrderStatus,
                             placedAt: Instant = Instant.now()
                            ) extends PlacedOrderTrait

object FailedPlacedOrder {
  /** Builds a failed placement model from an order intent and reason. */
  def apply(orderIntent: OrderIntent, reason: String): FailedPlacedOrder = {
    FailedPlacedOrder(
      reason = reason,
      price = orderIntent.price,
      quantity = orderIntent.quantity,
      side = orderIntent.side,
      symbol = orderIntent.symbol,
      status = OrderStatus.FAILED
    )
  }
}
