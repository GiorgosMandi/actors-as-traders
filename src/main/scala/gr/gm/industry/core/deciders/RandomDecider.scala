package gr.gm.industry.core.deciders

import gr.gm.industry.model.dao.PriceDao
import gr.gm.industry.utils.constants.TradeActions.{BUY, OMIT, SELL, TradeAction}

import scala.util.Random

object RandomDecider extends DecisionMaker {

    def decide(price: PriceDao): TradeAction = {
        new Random(System.currentTimeMillis).nextInt() % 6 match {
            case 1 => BUY
            case 2 => SELL
            case _ => OMIT
        }
    }
}
