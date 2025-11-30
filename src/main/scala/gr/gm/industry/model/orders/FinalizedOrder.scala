package gr.gm.industry.model.orders

import gr.gm.industry.model.ExecutionReport
import gr.gm.industry.utils.enums.{OrderStatus, Side, TimeInForce}
import gr.gm.industry.utils.model.TradingSymbol

import java.time.Instant

/**
 * Completed view of an order, combining submission details with execution report data.
 *
 * @param orderId        exchange order id.
 * @param clientOrderId  client id used when placing.
 * @param tradeId        trade identifier from execution report.
 * @param orderStatus    latest status from exchange.
 * @param price          submitted price.
 * @param quantity       submitted quantity.
 * @param side           BUY/SELL.
 * @param symbol         trading pair.
 * @param timeInForce    TIF used when placing.
 * @param finalStatus    status at finalization.
 * @param executedQty    quantity executed in last report.
 * @param executedPrice  price executed in last report.
 * @param placedAt       submission time.
 * @param finalizedAt    time derived from execution report.
 */
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
  /**
   * Convenience constructor combining the placed order and a received execution report.
   */
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
