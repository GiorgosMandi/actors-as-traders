package gr.gm.industry.model

import gr.gm.industry.utils.model.TradingSymbol

import java.time.LocalDateTime

case class CoinPrice(price: BigDecimal,
                     symbol: TradingSymbol,
                     timestamp: LocalDateTime
                    ) {
  override def toString: String = s"Symbol: ${symbol.toString()}: Price: $price"

}
