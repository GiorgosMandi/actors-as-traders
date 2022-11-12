package gr.gm.industry.deciders

import gr.gm.industry.dao.PriceDao
import gr.gm.industry.utils.constants.TradeActions.TradeAction

trait DecisionMaker {

    def decide(price: PriceDao): TradeAction
}
