package gr.gm.industry.core.traders

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import gr.gm.industry.api.BinanceWebClient
import gr.gm.industry.model.dao.Order.{BuyOrder, Order}
import gr.gm.industry.utils.Constants.{ACK, ETH}
import gr.gm.industry.utils.TradeActions.{BUY, PRINT, SELL}

import java.util.UUID

class SimpleTrader extends Actor with ActorLogging {

    import SimpleTrader.{Fail, Terminate}

    val MIN_DEPOSIT = 5f
    val MAX_CAPITAL = 300f
    val INITIAL_CAPITAL = 100f
    val PROFIT_BASE = 10f
    val BUY_RATIO_TO_CAPITAL = .2f

    val brokerActor: ActorRef = context.system.actorOf(Props[BinanceWebClient], "BinanceWebClientActor")

    override def receive: Receive = trade(INITIAL_CAPITAL, 0f, Map())

    def trade(capital: BigDecimal, deposits: BigDecimal, orders: Map[UUID, Order]): Receive = {
        /**
         * In case of a BUY, extract 20% of the current capital
         * and place an order. An order is defined as the
         * size of the investment and the given price
         */
        case (BUY, price: BigDecimal) =>
            val investment = capital * BUY_RATIO_TO_CAPITAL
            val capitalAfterOrder = capital - investment
            val buyOrder = BuyOrder(investment, ETH, price)
            brokerActor ! buyOrder
            context.become(trade(capitalAfterOrder, deposits, orders + (buyOrder.id -> buyOrder)))
            sender() ! ACK

        case (SELL, price: BigDecimal) =>

            /**
             * When we SELL, first we find the orders which are profitable if we sell them
             * with the given price. Then we compute the profit, based on the differences
             * of the BUY-ing price and the SELLing price.
             * TODO: sell message broker
             */
            val (profitablePurchases, rest) = orders.partition { case (_, order) => order.price < price }
            val previousInvestments = profitablePurchases.map(_._2.quantity).sum
            val profit = profitablePurchases
              .map { case (_, order) => (price * order.quantity) - (order.price * order.quantity) }
              .sum
            log.warning(s"SELLING: PROFIT: $profit")

            val newCapital = previousInvestments + capital
            val depositedCapital = depositCapital(newCapital, MAX_CAPITAL, MIN_DEPOSIT)
            val newDeposits = deposits + depositedCapital
            context.become(trade(newCapital - depositedCapital, newDeposits, rest))
            sender() ! ACK

        case PRINT =>
            print(capital, deposits, orders)
            sender() ! ACK

        case Fail(ex) =>
            log.warning(s"Stream Failed: $ex")
        case Terminate =>
            print(capital, deposits, orders)
            sender() ! ACK
            context.unbecome()
    }

    def depositProfit(profit: BigDecimal, base: BigDecimal, minDeposit: BigDecimal): BigDecimal = {
        val ratio = profit / (profit + base)
        val deposit = ratio * profit
        if (deposit > minDeposit) deposit else 0f
    }

    def depositCapital(capital: BigDecimal, maxCapital: BigDecimal, minDeposit: BigDecimal): BigDecimal = {
        if (capital > maxCapital) {
            val capitalBase = maxCapital * .5
            depositProfit(capital, capitalBase, minDeposit)
        }
        else
            0f
    }
}

object SimpleTrader {
    case object Init

    case object Terminate

    case class Fail(ex: Throwable)
}
