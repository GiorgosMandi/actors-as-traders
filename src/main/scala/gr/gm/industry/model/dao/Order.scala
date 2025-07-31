package gr.gm.industry.model.dao

import gr.gm.industry.utils.TradeActions.{BUY, OrderType, SELL}
import gr.gm.industry.utils.enums.Coin

import java.time.LocalDateTime
import java.util.UUID
import scala.math.BigDecimal.RoundingMode

object Order {
  trait Order {
    val id: UUID = UUID.randomUUID()
    val orderType: OrderType
    val quantity: BigDecimal
    val coin: Coin
    val price: BigDecimal
    val timestamp: LocalDateTime = LocalDateTime.now()

    def getQuantityInCoins: BigDecimal = price * quantity

    override def toString: String = s"Order $id: ${orderType.name} ${quantity.setScale(3, RoundingMode.HALF_EVEN)} ($coin, $price)"
  }

  case class BuyOrder(serverOrderId: Long, clientOrderId: String, quantity: BigDecimal, coin: Coin, price: BigDecimal) extends Order {
    val orderType: OrderType = BUY
  }

  case class SellOrder(serverOrderId: Long, clientOrderId: String, quantity: BigDecimal, coin: Coin, price: BigDecimal) extends Order {
    val orderType: OrderType = SELL
  }

  case class FailedOrder(err: String, orderType: OrderType, quantity: BigDecimal, coin: Coin, price: BigDecimal) extends Order
}

