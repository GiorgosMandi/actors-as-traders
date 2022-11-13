package gr.gm.industry.model.dao

import gr.gm.industry.utils.Constants.Coin
import gr.gm.industry.utils.TradeActions.OrderType

import java.util.UUID
import scala.math.BigDecimal.RoundingMode

case class Order(id: UUID, orderType: OrderType, investment: BigDecimal, coin: Coin, price: BigDecimal){
    override def toString: String = s"Order $id: ${orderType.name} ${investment.setScale(3, RoundingMode.HALF_EVEN)} ($coin, $price)"
}
