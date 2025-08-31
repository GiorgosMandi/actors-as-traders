package gr.gm.industry.model

import gr.gm.industry.utils.enums.{OrderStatus, Side}
import gr.gm.industry.utils.model.TradingSymbol


case class ExecutionReport(
                            eventType: String, // e
                            eventTime: Long, // E
                            symbol: TradingSymbol, // s
                            clientOrderId: String, // c
                            side: Side, // S (BUY/SELL)
                            orderType: String, // o (LIMIT/MARKET/..)
                            timeInForce: String, // f
                            orderQuantity: BigDecimal, // q
                            orderPrice: BigDecimal, // p
                            stopPrice: BigDecimal, // P
                            icebergQty: BigDecimal, // F
                            orderId: Long, // i
                            orderStatus: OrderStatus, // X (NEW/FILLED/...)
                            orderRejectReason: String, // r
                            orderReportType: String, // x (execution type)
                            lastExecutedQty: BigDecimal, // l
                            cumulativeFilledQty: BigDecimal, // z
                            lastExecutedPrice: BigDecimal, // L
                            commissionAmount: BigDecimal, // n
                            commissionAsset: Option[String], // N
                            transactionTime: Long, // T
                            tradeId: Long // t
                          )



