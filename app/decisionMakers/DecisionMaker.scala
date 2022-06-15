package decisionMakers

import utils.TradeActionT

trait DecisionMaker {

    def decide(price: Float): TradeActionT
}
