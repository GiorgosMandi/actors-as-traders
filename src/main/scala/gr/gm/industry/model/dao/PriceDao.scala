package gr.gm.industry.model.dao

import gr.gm.industry.model.dto.PriceDto
import gr.gm.industry.utils.Constants.{Coin, Currency, ETH}

import java.time.LocalDateTime
import scala.math.BigDecimal.RoundingMode

case class PriceDao(coin: Coin, currency: Currency, price: BigDecimal, timestamp: LocalDateTime){
    override def toString: String = s"Coin: $coin: Price: $price $currency"
}

object PriceDao {
    case class PriceError(message: String)


    def apply(priceDto: PriceDto): Either[PriceError, PriceDao] ={
        val price: BigDecimal = BigDecimal(priceDto.price).setScale(5, RoundingMode.HALF_EVEN)
        val coinOpt = Coin.get(priceDto.symbol.take(3))
        val currencyOpt = Currency.get(priceDto.symbol.takeRight(3))
        val priceOtp = coinOpt.zip(currencyOpt)
          .map {case (coin, currency) => PriceDao(coin, currency, price, LocalDateTime.now())}

        priceOtp match {
            case Some(price) => Right(price)
            case _ => Left(PriceError("Coin or currency not supported"))
        }

    }
}
