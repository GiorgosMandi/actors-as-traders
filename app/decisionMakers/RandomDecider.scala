package decisionMakers

import models.CoinGeckoResponse
import utils.constants.TradeAction.{BUY, OMIT, SELL}
import utils.constants.TradeAction.TradeActionT

import scala.util.Random

object RandomDecider extends DecisionMaker {

    def decide(price: CoinGeckoResponse): TradeActionT = {
        new Random(System.currentTimeMillis).nextInt() % 6 match {
            case 1 => BUY
            case 2 => SELL
            case _ => OMIT
        }
    }
}
