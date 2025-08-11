package gr.gm.industry.model

import gr.gm.industry.utils.enums.Side.{BUY, SELL}
import gr.gm.industry.utils.enums.{OrderStatus, Side}
import gr.gm.industry.utils.model.TradingSymbol

import java.time.LocalDateTime
import java.util.UUID
import scala.math.BigDecimal.RoundingMode

// todo replace this with new orders
object Order {
  trait Order {
    val id: UUID = UUID.randomUUID()
    val side: Side
    val tradingSymbol: TradingSymbol
    val quantity: BigDecimal
    val price: BigDecimal
    val orderStatus: OrderStatus = OrderStatus.NEW
    val createdAt: LocalDateTime = LocalDateTime.now()
    val updatedAt: LocalDateTime = LocalDateTime.now()

    def getQuantityInCoins: BigDecimal = price * quantity

    override def toString: String = s"Order $side: $side ${quantity.setScale(3, RoundingMode.HALF_EVEN)} ($tradingSymbol, $price)"
  }

  case class BuyOrder(serverOrderId: Long, clientOrderId: String, tradingSymbol: TradingSymbol, quantity: BigDecimal, price: BigDecimal) extends Order {
    val side: Side = BUY
  }

  case class SellOrder(serverOrderId: Long, clientOrderId: String, tradingSymbol: TradingSymbol, quantity: BigDecimal, price: BigDecimal) extends Order {
    val side: Side = SELL
  }

  case class FailedOrder(err: String, side: Side, tradingSymbol: TradingSymbol, quantity: BigDecimal, price: BigDecimal) extends Order
}

