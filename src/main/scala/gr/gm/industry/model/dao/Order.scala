package gr.gm.industry.model.dao

import gr.gm.industry.utils.Constants.Coin
import gr.gm.industry.utils.TradeActions.{BUY, OrderType, SELL}

import java.util.UUID
import scala.math.BigDecimal.RoundingMode

object Order {
    trait Order {
        val id: UUID = UUID.randomUUID()
        val orderType: OrderType
        val investment: BigDecimal
        val coin: Coin
        val price: BigDecimal

        override def toString: String = s"Order $id: ${orderType.name} ${investment.setScale(3, RoundingMode.HALF_EVEN)} ($coin, $price)"
    }

    case class BuyOrder(investment: BigDecimal, coin: Coin, price: BigDecimal) extends Order {
        val orderType: OrderType = BUY
    }

    case class SellOrder(investment: BigDecimal, coin: Coin, price: BigDecimal) extends Order {
        val orderType: OrderType = SELL
    }

}

