package gr.gm.industry.utils.jsonProtocols


import gr.gm.industry.dto.CoinGeckoPriceDto
import spray.json.{DefaultJsonProtocol, DeserializationException, JsObject, JsValue, RootJsonFormat}

object CoinGeckoPriceDtoProtocol extends DefaultJsonProtocol {
  implicit object CgEthInfoJsonReader extends RootJsonFormat[CoinGeckoPriceDto] {

    // example of the expected json:
    //"ethereum":{
    //  "eur":2280.34,
    //  "eur_market_cap":274647771967.14023,
    //  "eur_24h_vol":10710838487.83845,
    //  "eur_24h_change":1.5273056883490965}
    // }
    override def read(json: JsValue): CoinGeckoPriceDto = {
      json.asJsObject.getFields("ethereum") match {
        case Seq(JsObject(ethMap)) => CoinGeckoPriceDto(ethMap)
        case _ => throw DeserializationException("CgEthInfo expected")
      }
    }

    override def write(obj: CoinGeckoPriceDto): JsValue = ???
  }
}

