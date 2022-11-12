package gr.gm.industry.graphs.actors

import akka.actor.{Actor, ActorLogging}
import gr.gm.industry.utils.constants.TradeActions.{BUY, PRINT, SELL}

class SimpleTrader extends Actor with ActorLogging {

    val MIN_DEPOSIT = 5f
    val MAX_CAPITAL = 300f
    val INITIAL_CAPITAL = 100f
    val PROFIT_BASE = 10f

    override def receive: Receive = trade(INITIAL_CAPITAL, 0f, Nil)

    def trade(capital: BigDecimal, deposits: BigDecimal, orders: List[(BigDecimal, BigDecimal)]): Receive = {
        /**
         * In case of a BUY, extract 20% of the current capital
         *  and place an order. An order is defined as the
         *  size of the investment and the given price
         */
        case (BUY, price: BigDecimal) =>
            val investment = capital * .2
            val capitalAfterOrder = capital - investment
            val newOrder = (price, investment)

            log.warning(s"BUYING: $newOrder")
            context.become(trade(capitalAfterOrder, deposits, newOrder :: orders))

        case (SELL, price: BigDecimal) =>
            /**
             * When we SELL, first we find the orders which are profitable if we sell them
             * with the given price. Then we compute the profit, based on the differences
             * of the BUY-ing price and the SELLing price.
             */
            val (profitablePurchases, rest) = orders.partition(_._1 < price)
            val previousInvestments = profitablePurchases.map(_._2).sum
            val profit = profitablePurchases
              .map { case (previousPrice, investment) => (price * investment) - (previousPrice * investment) }
              .sum
            log.warning(s"SELLING: PROFIT: $profit")

            val depositedProfit = depositProfit(profit, PROFIT_BASE, MIN_DEPOSIT)
            val capitalizedProfit = profit - depositedProfit
            val newCapital = previousInvestments + capitalizedProfit + capital
            val depositedCapital = depositCapital(newCapital, MAX_CAPITAL, MIN_DEPOSIT)
            val newDeposits = deposits + depositedProfit + depositedCapital
            context.become(trade(newCapital-depositedCapital, newDeposits, rest))

        case PRINT =>
            log.warning(s"\n---\nCapital: $capital\nDeposits: $deposits\nExisting Purchases: $orders\n----")
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
