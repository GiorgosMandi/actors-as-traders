package gr.gm.industry.model

import gr.gm.industry.utils.model.TradingSymbol

import java.time.Instant

/**
 * Spot price snapshot for a trading symbol at a given instant.
 *
 * @param price     quoted price.
 * @param symbol    trading pair.
 * @param timestamp time the price was captured.
 */
case class CoinPrice(price: BigDecimal,
                     symbol: TradingSymbol,
                     timestamp: Instant
                    ) {
  override def toString: String = s"Symbol: $symbol: Price: $price"

}
