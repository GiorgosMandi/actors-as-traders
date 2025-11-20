package gr.gm.industry.model

import gr.gm.industry.utils.enums.{OrderStatus, Side}
import gr.gm.industry.utils.model.TradingSymbol


/**
 * Binance execution report payload.
 *
 * Field letters map to Binance user data stream keys.
 *
 * @param eventType           e, event name
 * @param eventTime           E, event timestamp (ms)
 * @param symbol              s, trading pair
 * @param clientOrderId       c, client-assigned id
 * @param side                S, BUY/SELL
 * @param orderType           o, LIMIT/MARKET/...
 * @param timeInForce         f, GTC/IOC/FOK
 * @param orderQuantity       q, original quantity
 * @param orderPrice          p, order limit price
 * @param stopPrice           P, stop price (for stop orders)
 * @param icebergQty          F, hidden iceberg size
 * @param orderId             i, Binance order id
 * @param orderStatus         X, NEW/FILLED/...
 * @param orderRejectReason   r, reject reason
 * @param orderReportType     x, execution type
 * @param lastExecutedQty     l, last fill quantity
 * @param cumulativeFilledQty z, total filled so far
 * @param lastExecutedPrice   L, last fill price
 * @param commissionAmount    n, commission amount
 * @param commissionAsset     N, commission asset
 * @param transactionTime     T, trade timestamp (ms)
 * @param tradeId             t, trade identifier
 */
case class ExecutionReport(
                            eventType: String,
                            eventTime: Long,
                            symbol: TradingSymbol,
                            clientOrderId: String,
                            side: Side,
                            orderType: String,
                            timeInForce: String,
                            orderQuantity: BigDecimal,
                            orderPrice: BigDecimal,
                            stopPrice: BigDecimal,
                            icebergQty: BigDecimal,
                            orderId: Long,
                            orderStatus: OrderStatus,
                            orderRejectReason: String,
                            orderReportType: String,
                            lastExecutedQty: BigDecimal,
                            cumulativeFilledQty: BigDecimal,
                            lastExecutedPrice: BigDecimal,
                            commissionAmount: BigDecimal,
                            commissionAsset: Option[String],
                            transactionTime: Long,
                            tradeId: Long
                          )

