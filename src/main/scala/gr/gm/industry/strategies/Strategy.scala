package gr.gm.industry.strategies

import gr.gm.industry.model.{CoinPrice, TradeDecision}

trait Strategy {

    def decide(price: CoinPrice): TradeDecision
}
