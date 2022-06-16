package graphs

import akka.actor.{Actor, ActorLogging}
import utils.TradeAction.{BUY, SELL}

class SimpleTrader extends Actor with ActorLogging {

    val MIN_CAPITAL = 5f
    val INITIAL_CAPITAL = 100f
    override def receive: Receive = trade(INITIAL_CAPITAL, 0f, Nil)

    // TODO Capitalize: if run out of capital transform a portion of the deposits into capital
    def trade(capital: Double, deposits: Double, purchases: List[(Double, Double)]): Receive = {
        case (BUY, price: Double) if capital * .2 > MIN_CAPITAL =>
            val investment = capital * .2
            val capitalAfterTrade = capital - investment
            val newPurchase = (price, investment)

            log.warning(s"BUYING: $newPurchase")
            context.become(trade(capitalAfterTrade, deposits, newPurchase :: purchases))

        case (SELL, price: Double) =>
            val (profitablePurchases, rest) = purchases.partition(_._1 < price)
            log.warning(s"SELLING: $profitablePurchases with price $price")

            // SELLING
            val profit = profitablePurchases
              .map { case (previousPrice, investment) => (price * investment) - (previousPrice * investment) }
              .sum

            val newCapital = profit + capital
            val newDeposits = computeDeposits(newCapital, INITIAL_CAPITAL * .2f)
            context.become(trade(newCapital-newDeposits, newDeposits + deposits, rest))
        case "print" =>
            log.warning(s"\n---\nCapital: $capital\nDeposits: $deposits\nExisting Purchases: $purchases\n\n")
    }

    def computeDeposits(capital: Double, limit: Double): Double = {
        if (capital > limit) {
            return limit
        }
        0f
    }
}
