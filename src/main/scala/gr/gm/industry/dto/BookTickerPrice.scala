package gr.gm.industry.dto

case class BookTickerPrice(
                            updateId: Long,
                            symbol: String,
                            bestBidPrice: BigDecimal, // The highest price a buyer is willing to pay for an asset
                            bestBidQty: BigDecimal, // the quantity willing to purchase with bestBidPrice
                            bestAskPrice: BigDecimal, // The lowest price a seller is willing to accept for an asset.
                            bestAskQty: BigDecimal // the quantity willing to sell with bestAskPrice
                          )


