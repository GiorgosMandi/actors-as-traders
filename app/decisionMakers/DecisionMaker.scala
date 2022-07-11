package decisionMakers

import models.CoinGeckoResponse
import utils.constants.TradeAction.TradeActionT

trait DecisionMaker {

    def decide(price: CoinGeckoResponse): TradeActionT
}
