package decisionMakers

import utils.TradeActionT
import utils.TradeAction.{BUY, OMIT, SELL}

import scala.util.Random

object RandomDecider extends DecisionMaker {

    def decide(price: Float): TradeActionT = {
        new Random().nextInt(new java.util.Date().hashCode) % 6 match {
            case 1 => BUY
            case 2 => SELL
            case _ => OMIT
        }
    }
}
