package gr.gm.industry.model.dao

import gr.gm.industry.model.dto.PriceDto
import java.sql.Timestamp
import java.text.SimpleDateFormat
import scala.math.BigDecimal.RoundingMode

case class PriceDao(coin: String, currency: String, price: BigDecimal, timestamp: String){
    override def toString: String = s"$timestamp - $coin: $price $currency"
}

object PriceDao {
    val sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

    def apply(priceDto: PriceDto): PriceDao ={
        val timestamp = sdf.format(new Timestamp(System.currentTimeMillis()))
        val symbol = priceDto.symbol.take(3)
        val currency = priceDto.symbol.takeRight(3)
        val price: BigDecimal = BigDecimal(priceDto.price).setScale(5, RoundingMode.HALF_EVEN)
        PriceDao(symbol, currency, price, timestamp)
    }
}
