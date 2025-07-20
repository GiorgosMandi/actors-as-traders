package gr.gm.industry.core.deciders

import gr.gm.industry.core.traders.NaivePendingTrader.{Buy, Omit, Sell, TradeActionEvent}
import gr.gm.industry.model.dao.CoinPrice

import scala.util.Random

object RandomDecider extends DecisionMaker {

    def decide(price: CoinPrice): TradeActionEvent = {
        new Random(System.currentTimeMillis).nextInt() % 6 match {
            case 1 => Buy(price)
            case 2 => Sell(price)
            case _ => Omit
        }
    }
}
