package gr.gm.industry.utils.jsonProtocols

import gr.gm.industry.dto.BookTickerPrice
import spray.json.{DefaultJsonProtocol, DeserializationException, JsNumber, JsObject, JsString, JsValue, RootJsonFormat}


object BookTickerPriceProtocol extends DefaultJsonProtocol {
  implicit val binanceDepthUpdateFormat: RootJsonFormat[BookTickerPrice] = new RootJsonFormat[BookTickerPrice] {
    override def write(obj: BookTickerPrice): JsValue = JsObject(
      "u" -> JsNumber(obj.updateId),
      "b" -> JsString(obj.bestBidPrice.toString()),
      "B" -> JsString(obj.bestBidQty.toString()),
      "a" -> JsString(obj.bestAskPrice.toString()),
      "A" -> JsString(obj.bestAskQty.toString())
    )

    override def read(json: JsValue): BookTickerPrice = {
      json.asJsObject.getFields("u", "s", "b", "B", "a", "A") match {
        case Seq(JsNumber(u), JsString(s), JsString(b), JsString(bbq), JsString(a), JsString(baq)) =>
          BookTickerPrice(u.toLong, s, BigDecimal(b), BigDecimal(bbq), BigDecimal(a), BigDecimal(baq))
        case _ => throw DeserializationException("BinanceDepthUpdate expected")
      }
    }
  }
}
