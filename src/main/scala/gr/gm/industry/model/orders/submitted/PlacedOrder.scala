package gr.gm.industry.model.orders.submitted

import gr.gm.industry.model.TradeDecision.OrderIntent
import gr.gm.industry.utils.enums.{OrderStatus, Side}
import gr.gm.industry.utils.model.TradingSymbol

import java.time.LocalDateTime


case class PlacedOrder(
                        orderId: Long,
                        clientOrderId: String,
                        price: BigDecimal,
                        quantity: BigDecimal,
                        side: Side,
                        symbol: TradingSymbol,
                        status: OrderStatus,
                        placedAt: LocalDateTime = LocalDateTime.now()
                      ) extends SubmittedOrder

  object PlacedOrder {
  def apply(orderIntent: OrderIntent, orderId: Long, clientOrderId: String): PlacedOrder = {
    PlacedOrder(
      orderId = orderId,
      clientOrderId = clientOrderId,
      price = orderIntent.price,
      quantity = orderIntent.quantity,
      side = orderIntent.side,
      symbol = orderIntent.symbol,
      status = OrderStatus.NEW
    )
  }
}

