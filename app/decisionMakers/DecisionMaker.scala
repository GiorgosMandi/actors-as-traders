package decisionMakers

import models.CoinGeckoResponse
import utils.constants.TradeActionT

trait DecisionMaker {

    def decide(price: CoinGeckoResponse): TradeActionT
}
