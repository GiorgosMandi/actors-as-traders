package gr.gm.industry.utils

import gr.gm.industry.model.dao.Order
import gr.gm.industry.utils.Constants.Coin

import java.util.UUID

object TradeActions {

    sealed trait TradeAction

    sealed trait OrderType extends TradeAction {
        val name = ""
    }
    case object BUY extends OrderType {
        override val name = "BUY"
    }
    case object SELL extends OrderType {
        override val name = "SELL"
    }
    case object OMIT extends TradeAction
    case object PRINT extends TradeAction

    def elect(tradeActions: Product): TradeAction ={
        val tradeActionsList = tradeActions.productIterator.map(_.asInstanceOf[TradeAction]).toList
        val votes = tradeActionsList.foldLeft(Map[TradeAction,Int]().withDefaultValue(0)){
            case (acc, letter) => acc + (letter -> (1 + acc(letter)))
        }
        votes.maxBy(_._2)._1
    }

    def buy(price: BigDecimal, quantity: BigDecimal, coin: Coin): Order = {
        // TODO: the whole procedure must be async
        val order = Order(UUID.randomUUID(), BUY, quantity, coin, price)
        // TODO:
        //  - request to place order
        //  - wait for response
        //  - wait when order has fulfilled
        Thread.sleep(10000)
        order
    }
}


