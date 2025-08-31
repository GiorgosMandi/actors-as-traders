package gr.gm.industry.model.orders.submitted

import gr.gm.industry.model.TradeDecision.OrderIntent
import gr.gm.industry.utils.enums.{OrderStatus, Side}
import gr.gm.industry.utils.model.TradingSymbol

import java.time.Instant

case class FailedPlacedOrder(reason: String,
                             price: BigDecimal,
                             quantity: BigDecimal,
                             side: Side,
                             symbol: TradingSymbol,
                             status: OrderStatus,
                             placedAt: Instant = Instant.now()
                            ) extends PlacedOrderTrait

object FailedPlacedOrder {
  def apply(orderIntent: OrderIntent, reason: String): FailedPlacedOrder = {
    FailedPlacedOrder(
      reason = reason,
      price = orderIntent.price,
      quantity = orderIntent.quantity,
      side = orderIntent.side,
      symbol = orderIntent.symbol,
      status = OrderStatus.NEW
    )
  }
}