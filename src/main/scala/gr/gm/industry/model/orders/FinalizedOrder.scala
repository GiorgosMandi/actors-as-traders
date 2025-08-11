package gr.gm.industry.model.orders

import gr.gm.industry.model.orders.submitted.PlacedOrder
import gr.gm.industry.utils.enums.{OrderStatus, Side}
import gr.gm.industry.utils.model.TradingSymbol

import java.time.LocalDateTime

case class FinalizedOrder(
                           orderId: Long,
                           clientOrderId: String,
                           price: BigDecimal,
                           quantity: BigDecimal,
                           side: Side,
                           symbol: TradingSymbol,
                           finalStatus: OrderStatus,
                           executedQty: BigDecimal,
                           placedAt: LocalDateTime,
                           finalizedAt: LocalDateTime = LocalDateTime.now()
                         ) {
  
  
}

object FinalizedOrder {
  def apply(placedOrder: PlacedOrder, finalStatus: OrderStatus, executedQty: BigDecimal): FinalizedOrder = {
    FinalizedOrder(
      orderId = placedOrder.orderId,
      clientOrderId = placedOrder.clientOrderId,
      price = placedOrder.price,
      quantity = placedOrder.quantity,
      side = placedOrder.side,
      symbol = placedOrder.symbol,
      finalStatus = finalStatus,
      executedQty = executedQty,
      placedAt = placedOrder.placedAt
    )
  }
}
