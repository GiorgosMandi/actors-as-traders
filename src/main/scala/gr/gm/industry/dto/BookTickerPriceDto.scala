package gr.gm.industry.dto

import gr.gm.industry.utils.model.TradingSymbol

import java.time.LocalDateTime

case class BookTickerPriceDto(
                               updateId: Long,
                               symbol: TradingSymbol,
                               bestBidPrice: BigDecimal, // The highest price a buyer is willing to pay for an asset
                               bestBidQty: BigDecimal, // the quantity willing to purchase with bestBidPrice
                               bestAskPrice: BigDecimal, // The lowest price a seller is willing to accept for an asset.
                               bestAskQty: BigDecimal, // the quantity willing to sell with bestAskPrice
                               timestamp: LocalDateTime = LocalDateTime.now()
                             )


