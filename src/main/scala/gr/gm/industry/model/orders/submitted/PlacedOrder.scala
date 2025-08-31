package gr.gm.industry.model.orders.submitted

import gr.gm.industry.model.TradeDecision.OrderIntent
import gr.gm.industry.utils.enums.{OrderStatus, Side, TimeInForce}
import gr.gm.industry.utils.model.TradingSymbol

import java.time.Instant


case class PlacedOrder(
                        orderId: Long,
                        clientOrderId: String,
                        timeInForce: TimeInForce,
                        price: BigDecimal,
                        quantity: BigDecimal,
                        side: Side,
                        symbol: TradingSymbol,
                        status: OrderStatus,
                        placedAt: Instant = Instant.now()
                      ) extends PlacedOrderTrait

  object PlacedOrder {
  def apply(orderIntent: OrderIntent, orderId: Long, clientOrderId: String, timeInForce: TimeInForce): PlacedOrder = {
    PlacedOrder(
      orderId = orderId,
      clientOrderId = clientOrderId,
      timeInForce = timeInForce,
      price = orderIntent.price,
      quantity = orderIntent.quantity,
      side = orderIntent.side,
      symbol = orderIntent.symbol,
      status = OrderStatus.NEW
    )
  }
}

