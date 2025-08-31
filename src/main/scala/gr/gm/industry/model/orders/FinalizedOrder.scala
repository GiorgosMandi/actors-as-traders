package gr.gm.industry.model.orders

import gr.gm.industry.model.ExecutionReport
import gr.gm.industry.model.orders.submitted.PlacedOrder
import gr.gm.industry.utils.enums.{OrderStatus, Side, TimeInForce}
import gr.gm.industry.utils.model.TradingSymbol

import java.time.Instant

case class FinalizedOrder(
                           orderId: Long,
                           clientOrderId: String,
                           tradeId: Long,
                           orderStatus: OrderStatus,
                           price: BigDecimal,
                           quantity: BigDecimal,
                           side: Side,
                           symbol: TradingSymbol,
                           timeInForce: TimeInForce,
                           finalStatus: OrderStatus,
                           executedQty: BigDecimal,
                           executedPrice: BigDecimal,
                           placedAt: Instant,
                           finalizedAt: Instant
                         ) {


}

object FinalizedOrder {
  // todo
  def apply(placedOrder: PlacedOrder, report: ExecutionReport): FinalizedOrder = {
    FinalizedOrder(
      orderId = placedOrder.orderId,
      clientOrderId = placedOrder.clientOrderId,
      tradeId = report.orderId,
      orderStatus = report.orderStatus,
      price = placedOrder.price,
      quantity = placedOrder.quantity,
      side = placedOrder.side,
      symbol = placedOrder.symbol,
      timeInForce = placedOrder.timeInForce,
      finalStatus = report.orderStatus,
      executedQty = report.lastExecutedQty,
      executedPrice = report.lastExecutedPrice,
      placedAt = placedOrder.placedAt,
      finalizedAt = Instant.ofEpochMilli(report.transactionTime)
    )
  }
}
