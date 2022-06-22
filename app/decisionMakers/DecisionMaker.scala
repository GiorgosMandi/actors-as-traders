package decisionMakers

import utils.constants.TradeActionT

trait DecisionMaker {

    def decide(price: Float): TradeActionT
}
