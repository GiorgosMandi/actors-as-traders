package gr.gm.industry.strategies

import gr.gm.industry.dto.BookTickerPriceDto
import gr.gm.industry.model.TradeDecision

trait Strategy {

    def decide(price: BookTickerPriceDto): TradeDecision
}
