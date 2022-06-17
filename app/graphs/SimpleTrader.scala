package graphs

import akka.actor.{Actor, ActorLogging}
import utils.TradeAction.{BUY, SELL}

class SimpleTrader extends Actor with ActorLogging {

    val MIN_DEPOSIT = 5f
    val MAX_CAPITAL = 300f
    val INITIAL_CAPITAL = 100f
    val PROFIT_BASE = 10f


    override def receive: Receive = trade(INITIAL_CAPITAL, 0f, Nil)

    def trade(capital: Double, deposits: Double, purchases: List[(Double, Double)]): Receive = {
        case (BUY, price: Double) =>
            val investment = capital * .2
            val capitalAfterTrade = capital - investment
            val newPurchase = (price, investment)

            log.warning(s"BUYING: $newPurchase")
            context.become(trade(capitalAfterTrade, deposits, newPurchase :: purchases))

        case (SELL, price: Double) =>
            // SELLING
            val (profitablePurchases, rest) = purchases.partition(_._1 < price)
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
        case "print" =>
            log.warning(s"\n---\nCapital: $capital\nDeposits: $deposits\nExisting Purchases: $purchases\n\n")
    }

    def depositProfit(profit: Double, base: Double, minDeposit: Double): Double = {
        val ratio = profit/ (profit+base)
        val deposit =  ratio*profit
        if (deposit > minDeposit) deposit else 0f
    }

    def depositCapital(capital: Double, maxCapital: Double, minDeposit: Double): Double ={
        if (capital > maxCapital){
            val capitalBase = maxCapital * .5
            depositProfit(capital, capitalBase, minDeposit)
        }
        else
            0f
    }
}
