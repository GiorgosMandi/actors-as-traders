package gr.gm.industry.core.deciders

import gr.gm.industry.core.traders.NaivePendingTrader.TradeActionEvent
import gr.gm.industry.model.dao.CoinPrice

trait DecisionMaker {

    def decide(price: CoinPrice): TradeActionEvent
}
