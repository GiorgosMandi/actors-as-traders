package gr.gm.industry.core.deciders

import gr.gm.industry.model.{CoinPrice, TradeDecision}

trait DecisionMaker {

    def decide(price: CoinPrice): TradeDecision
}
