package gr.gm.industry.core.traders

import akka.actor.{ActorLogging, ActorRef, FSM, Props}
import gr.gm.industry.api.BinanceWebClient
import gr.gm.industry.core.traders.NaivePendingTrader.TraderError.NOT_INITIALIZED
import gr.gm.industry.core.traders.NaivePendingTrader.{Buy, Idle, Initialize, Operational, Pending, PendingOrder, Sell, SellOff, TraderData, TraderError, TraderState, Uninitialized}
import gr.gm.industry.model.dao.Order.{BuyOrder, Order, SellOrder}
import gr.gm.industry.model.dao.PriceDao
import gr.gm.industry.utils.Constants.ACK

// TODO
//  - Pending State
//  - add logs
//  - Deposit in every sell
//  - Deposit command
//  - Print command
//  - Store orders in db after fulfillment -
class NaivePendingTrader extends FSM[TraderState, TraderData] with ActorLogging {

    val MIN_DEPOSIT = 5f
    val BUY_RATIO_TO_CAPITAL = .2f

    val brokerActor: ActorRef = context.system.actorOf(Props[BinanceWebClient], "BinanceWebClientActor")

    startWith(Idle, Uninitialized)

    when(Idle) {
        case Event(Initialize(capital, executedOrders), Uninitialized) =>
            log.info(s"Changing state into operational with capital: $capital")
            goto(Operational) using Initialize(capital, executedOrders)
        case x =>
            log.error(s"Not expecting message ${x.toString}")
            sender() ! TraderError(NOT_INITIALIZED)
            stay()
    }

    when(Operational) {
        case Event(Buy(priceDao: PriceDao), Initialize(capital, purchases)) =>
            val investment = capital * BUY_RATIO_TO_CAPITAL
            val capitalAfterOrder = capital - investment
            val buyOrder = BuyOrder(investment, priceDao.coin, priceDao.price)
            brokerActor ! buyOrder
            sender() ! ACK
            goto(Pending) using PendingOrder(buyOrder, capitalAfterOrder, purchases)

        case Event(Sell(priceDao: PriceDao), Initialize(capital, purchases)) =>
            /**
             * When we SELL, first we find the orders which are profitable if we sell them
             * with the given price. Then we compute the profit, based on the differences
             * of the BUY-ing price and the SELLing price.
             */
            val (profitablePurchases, restPurchases) = purchases
              .partition { order => order.price < priceDao.price }
            val sellingQuantity = profitablePurchases.map(order => order.getQuantityInCoins).sum
            val expectedProfit = profitablePurchases
              .map(order => (priceDao.price * order.quantity) - (order.price * order.quantity) )
              .sum
            log.info(s"SELLING: Expected profit: $expectedProfit")
            val capitalAfterOrder = expectedProfit + capital
            val sellOrder = SellOrder(sellingQuantity, priceDao.coin, priceDao.price)
            brokerActor ! sellOrder
            sender() ! ACK
            goto(Pending) using PendingOrder(sellOrder, capitalAfterOrder, restPurchases)

        case Event(SellOff(priceDao: PriceDao), Initialize(capital, purchases)) =>
            val sellingQuantity: BigDecimal = purchases.map(order => order.getQuantityInCoins).sum
            val expectedProfit = purchases
              .map(order => (priceDao.price * order.quantity) - (order.price * order.quantity))
              .sum
            log.info(s"SELL OFF: Expected profit: $expectedProfit")
            val capitalAfterOrder: BigDecimal = expectedProfit + capital
            val sellOrder = SellOrder(sellingQuantity, priceDao.coin, priceDao.price)
            brokerActor ! sellOrder
            sender() ! ACK
            goto(Pending) using PendingOrder(sellOrder, capitalAfterOrder, Nil)
    }
}

object NaivePendingTrader {

    trait TraderState
    case object Idle extends TraderState
    case object Operational extends TraderState
    case object Pending extends TraderState

    trait TraderData
    case object Uninitialized extends TraderData
    case class Initialize(capital: BigDecimal, executedOrders: List[BuyOrder]) extends TraderData
    case class PendingOrder(order: Order, capital: BigDecimal, executedOrders: List[BuyOrder]) extends TraderData

    // expected messages
    case class TraderError(reason: String)
    case class Buy(priceDao: PriceDao)
    case class Sell(priceDao: PriceDao)
    case class SellOff(priceDao: PriceDao)


    object TraderError {
        val NOT_INITIALIZED = "Trader not initialized"
    }

}
