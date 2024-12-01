package gr.gm.industry.core.deciders

import gr.gm.industry.core.traders.NaivePendingTrader.{Buy, Omit, Sell, TraderEvent}
import gr.gm.industry.model.dao.CoinPrice

import scala.util.Random

object RandomDecider extends DecisionMaker {

    def decide(price: CoinPrice): TraderEvent = {
        new Random(System.currentTimeMillis).nextInt() % 6 match {
            case 1 => Buy(price)
            case 2 => Sell(price)
            case _ => Omit
        }
    }
}
