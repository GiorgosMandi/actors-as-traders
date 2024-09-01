package gr.gm.industry.utils.reader;


import gr.gm.industry.model.dao.CgEthInfo
import spray.json.{DefaultJsonProtocol, DeserializationException, JsObject, JsValue, RootJsonFormat};

object JsonReaders extends DefaultJsonProtocol {
  implicit object CgEthInfoJsonReader extends RootJsonFormat[CgEthInfo] {

    // example of the expected json:
    //"ethereum":{
    //  "eur":2280.34,
    //  "eur_market_cap":274647771967.14023,
    //  "eur_24h_vol":10710838487.83845,
    //  "eur_24h_change":1.5273056883490965}
    // }
    override def read(json: JsValue): CgEthInfo = {
      json.asJsObject.getFields("ethereum") match {
        case Seq(JsObject(ethMap)) => CgEthInfo(ethMap)
        case _ => throw DeserializationException("CgEthInfo expected")
      }
    }

    override def write(obj: CgEthInfo): JsValue = ???
  }
}

