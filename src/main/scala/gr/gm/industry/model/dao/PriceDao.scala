package gr.gm.industry.model.dao

import gr.gm.industry.model.dto.PriceDto
import gr.gm.industry.utils.Constants.{Coin, Currency, ETH}

import java.sql.Timestamp
import java.text.SimpleDateFormat
import scala.math.BigDecimal.RoundingMode

case class PriceDao(coin: Coin, currency: Currency, price: BigDecimal, timestamp: String){
    override def toString: String = s"Coin: $coin: Price: $price $currency"
}

object PriceDao {
    case class PriceError(message: String)

    val sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

    def apply(priceDto: PriceDto): Either[PriceError, PriceDao] ={
        val timestamp = sdf.format(new Timestamp(System.currentTimeMillis()))
        val price: BigDecimal = BigDecimal(priceDto.price).setScale(5, RoundingMode.HALF_EVEN)
        val coinOpt = Coin.get(priceDto.symbol.take(3))
        val currencyOpt = Currency.get(priceDto.symbol.takeRight(3))
        val priceOtp = coinOpt.zip(currencyOpt)
          .map {case (coin, currency) => PriceDao(coin, currency, price, timestamp)}

        priceOtp match {
            case Some(price) => Right(price)
            case _ => Left(PriceError("Coin or currency not supported"))
        }

    }
}
