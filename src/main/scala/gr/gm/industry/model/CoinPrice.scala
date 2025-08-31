package gr.gm.industry.model

import gr.gm.industry.utils.model.TradingSymbol

import java.time.Instant

case class CoinPrice(price: BigDecimal,
                     symbol: TradingSymbol,
                     timestamp: Instant
                    ) {
  override def toString: String = s"Symbol: ${symbol.toString()}: Price: $price"

}
