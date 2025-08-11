package gr.gm.industry.model

import gr.gm.industry.dto.BinancePriceDto
import gr.gm.industry.utils.enums.{Coin, Currency}
import gr.gm.industry.utils.model.TradingSymbol
import java.time.LocalDateTime
import scala.math.BigDecimal.RoundingMode

case class CoinPrice(price: BigDecimal,
                     symbol: TradingSymbol,
                     timestamp: LocalDateTime
                    ) {
  override def toString: String = s"Symbol: ${symbol.toString()}: Price: $price"

}

object CoinPrice {
  case class PriceError(message: String)

  def apply(priceDto: BinancePriceDto): Either[PriceError, CoinPrice] = {
    val price: BigDecimal = BigDecimal(priceDto.price).setScale(5, RoundingMode.HALF_EVEN)
    val coinOpt = Coin.get(priceDto.symbol.take(3))
    val currencyOpt = Currency.get(priceDto.symbol.takeRight(3))
    val priceOtp = coinOpt.zip(currencyOpt)
      .map { case (coin, currency) =>
        CoinPrice(price, TradingSymbol(coin, currency), LocalDateTime.now())
      }
    priceOtp match {
      case Some(price) => Right(price)
      case _ => Left(PriceError("Coin or currency not supported"))
    }

  }
}
