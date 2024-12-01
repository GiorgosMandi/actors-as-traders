package gr.gm.industry.core.deciders

import gr.gm.industry.core.traders.NaivePendingTrader.TraderEvent
import gr.gm.industry.model.dao.CoinPrice

trait DecisionMaker {

    def decide(price: CoinPrice): TraderEvent
}
