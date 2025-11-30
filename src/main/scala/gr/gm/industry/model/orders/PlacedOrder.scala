package gr.gm.industry.model.orders

import gr.gm.industry.model.TradeDecision.OrderIntent
import gr.gm.industry.utils.enums.{OrderStatus, Side, TimeInForce}
import gr.gm.industry.utils.model.TradingSymbol

import java.time.Instant


/**
 * Order successfully accepted by the exchange.
 *
 * @param orderId       exchange-generated id.
 * @param clientOrderId client-provided id.
 * @param timeInForce   time in force policy.
 * @param price         limit price submitted.
 * @param quantity      quantity submitted.
 * @param side          BUY/SELL side.
 * @param symbol        trading pair.
 * @param status        current status (initially NEW).
 * @param placedAt      timestamp of submission.
 */
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
  /**
   * Builds a placed order model from an intent and exchange identifiers.
   */
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
