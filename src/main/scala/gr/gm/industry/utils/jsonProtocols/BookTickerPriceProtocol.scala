package gr.gm.industry.utils.jsonProtocols

import gr.gm.industry.dto.BookTickerPriceDto
import gr.gm.industry.utils.model.TradingSymbol
import spray.json.{DefaultJsonProtocol, DeserializationException, JsNumber, JsObject, JsString, JsValue, RootJsonFormat}


object BookTickerPriceProtocol extends DefaultJsonProtocol {
  implicit val binanceDepthUpdateFormat: RootJsonFormat[BookTickerPriceDto] = new RootJsonFormat[BookTickerPriceDto] {
    override def write(obj: BookTickerPriceDto): JsValue = JsObject(
      "u" -> JsNumber(obj.updateId),
      "b" -> JsString(obj.bestBidPrice.toString()),
      "B" -> JsString(obj.bestBidQty.toString()),
      "a" -> JsString(obj.bestAskPrice.toString()),
      "A" -> JsString(obj.bestAskQty.toString())
    )

    override def read(json: JsValue): BookTickerPriceDto = {
      json.asJsObject.getFields("u", "s", "b", "B", "a", "A") match {
        case Seq(JsNumber(u), JsString(s), JsString(b), JsString(bbq), JsString(a), JsString(baq)) =>
          BookTickerPriceDto(u.toLong, TradingSymbol(s), BigDecimal(b), BigDecimal(bbq), BigDecimal(a), BigDecimal(baq))
        case _ => throw DeserializationException("BinanceDepthUpdate expected")
      }
    }
  }
}
