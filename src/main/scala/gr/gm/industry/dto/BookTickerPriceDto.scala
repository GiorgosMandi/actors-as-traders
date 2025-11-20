package gr.gm.industry.dto

import gr.gm.industry.utils.model.TradingSymbol

import java.time.Instant

/**
 * Binance bookTicker snapshot for a trading symbol.
 *
 * @param updateId     stream update identifier.
 * @param symbol       trading pair (e.g., BTCUSDT).
 * @param bestBidPrice highest price a buyer is willing to pay.
 * @param bestBidQty   quantity available at the best bid price.
 * @param bestAskPrice lowest price a seller is willing to accept.
 * @param bestAskQty   quantity available at the best ask price.
 * @param timestamp    capture time (default: now).
 */
case class BookTickerPriceDto(
                               updateId: Long,
                               symbol: TradingSymbol,
                               bestBidPrice: BigDecimal,
                               bestBidQty: BigDecimal,
                               bestAskPrice: BigDecimal,
                               bestAskQty: BigDecimal,
                               timestamp: Instant = Instant.now()
                             )

