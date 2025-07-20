package gr.gm.industry.model.dao

import gr.gm.industry.model.dto.PriceDto
import gr.gm.industry.utils.enums.{Coin, Currency}
import reactivemongo.api.bson.{BSONDocumentHandler, BSONObjectID, Macros}

import java.time.LocalDateTime
import scala.math.BigDecimal.RoundingMode

case class CoinPrice(_id: Option[BSONObjectID],
                     coin: Coin,
                     currency: Currency,
                     price: BigDecimal,
                     timestamp: LocalDateTime
                    ) {
  override def toString: String = s"Coin: $coin: Price: $price $currency"
}

object CoinPrice {
  case class PriceError(message: String)

  // This is needed for BSON serialization
  implicit val coinPriceHandler: BSONDocumentHandler[CoinPrice] = Macros.handler[CoinPrice]

  def apply(priceDto: PriceDto): Either[PriceError, CoinPrice] = {
    val price: BigDecimal = BigDecimal(priceDto.price).setScale(5, RoundingMode.HALF_EVEN)
    val coinOpt = Coin.get(priceDto.symbol.take(3))
    val currencyOpt = Currency.get(priceDto.symbol.takeRight(3))
    val priceOtp = coinOpt.zip(currencyOpt)
      .map { case (coin, currency) =>
        CoinPrice(Option.empty, coin, currency, price, LocalDateTime.now())
      }
    priceOtp match {
      case Some(price) => Right(price)
      case _ => Left(PriceError("Coin or currency not supported"))
    }

  }
}
