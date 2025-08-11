package gr.gm.industry.dto

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

// price fetched from binance
case class BinancePriceDto(price: String, symbol: String)

object BinancePriceDto extends SprayJsonSupport with DefaultJsonProtocol {
    implicit val priceSymbolJsonFormat: RootJsonFormat[BinancePriceDto] = jsonFormat2(BinancePriceDto.apply)
}