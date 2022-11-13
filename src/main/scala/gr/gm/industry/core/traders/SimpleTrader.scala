package gr.gm.industry.core.traders

import akka.actor.{Actor, ActorLogging}
import gr.gm.industry.model.dao.Order
import gr.gm.industry.utils.Constants.ETH
import gr.gm.industry.utils.TradeActions.{BUY, PRINT, SELL, buy}

import java.util.UUID

class SimpleTrader extends Actor with ActorLogging {
    import SimpleTrader.{Terminate, Fail, ACK}

    val MIN_DEPOSIT = 5f
    val MAX_CAPITAL = 300f
    val INITIAL_CAPITAL = 100f
    val PROFIT_BASE = 10f
    val BUY_RATIO_TO_CAPITAL = .2f

    override def receive: Receive = trade(INITIAL_CAPITAL, 0f, Map())

    def trade(capital: BigDecimal, deposits: BigDecimal, orders: Map[UUID, Order]): Receive = {
        /**
         * In case of a BUY, extract 20% of the current capital
         *  and place an order. An order is defined as the
         *  size of the investment and the given price
         */
        case (BUY, price: BigDecimal) =>
            val quantity = capital * BUY_RATIO_TO_CAPITAL
            val capitalAfterOrder = capital - quantity
            val order = buy(price, quantity, ETH)

            log.info(order.toString)
            context.become(trade(capitalAfterOrder, deposits, orders + (order.id -> order)))
            sender() ! ACK

        case (SELL, price: BigDecimal) =>
            /**
             * When we SELL, first we find the orders which are profitable if we sell them
             * with the given price. Then we compute the profit, based on the differences
             * of the BUY-ing price and the SELLing price.
             */
            val (profitablePurchases, rest) = orders.partition { case (_, order) => order.price < price }
            val previousInvestments = profitablePurchases.map(_._2.investment).sum
            val profit = profitablePurchases
              .map { case (_, order) => (price * order.investment) - (order.price * order.investment) }
              .sum
            log.warning(s"SELLING: PROFIT: $profit")

            val newCapital = previousInvestments + capital
            val depositedCapital = depositCapital(newCapital, MAX_CAPITAL, MIN_DEPOSIT)
            val newDeposits = deposits + depositedCapital
            context.become(trade(newCapital-depositedCapital, newDeposits, rest))
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
        val ratio = profit/ (profit+base)
        val deposit =  ratio*profit
        if (deposit > minDeposit) deposit else 0f
    }

    def depositCapital(capital: BigDecimal, maxCapital: BigDecimal, minDeposit: BigDecimal): BigDecimal ={
        if (capital > maxCapital){
            val capitalBase = maxCapital * .5
            depositProfit(capital, capitalBase, minDeposit)
        }
        else
            0f
    }
}

object SimpleTrader{
    case object Init
    case object ACK
    case object Terminate
    case class Fail(ex: Throwable)
}
