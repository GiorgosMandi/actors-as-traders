package gr.gm.industry.model.dto

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

case class PriceDto(price: String, symbol: String)

object PriceDto extends SprayJsonSupport with DefaultJsonProtocol {
    implicit val priceSymbolJsonFormat: RootJsonFormat[PriceDto] = jsonFormat2(PriceDto.apply)
}

