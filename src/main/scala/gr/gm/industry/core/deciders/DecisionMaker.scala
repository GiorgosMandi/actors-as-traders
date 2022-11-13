package gr.gm.industry.core.deciders

import gr.gm.industry.model.dao.PriceDao
import gr.gm.industry.utils.TradeActions.TradeAction

trait DecisionMaker {

    def decide(price: PriceDao): TradeAction
}
