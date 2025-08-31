package gr.gm.industry.protocol

import gr.gm.industry.model.ExecutionReport
import gr.gm.industry.utils.enums.{OrderStatus, Side}
import gr.gm.industry.utils.model.TradingSymbol
import spray.json._

object ExecutionReportProtocol extends DefaultJsonProtocol {
  implicit val bigDecimalFormat: JsonFormat[BigDecimal] = new JsonFormat[BigDecimal] {
    def write(x: BigDecimal) = JsString(x.toString)

    def read(value: JsValue): BigDecimal = value match {
      case JsString(str) => BigDecimal(str)
      case JsNumber(num) => num
      case _ => deserializationError("Expected BigDecimal as JsString or JsNumber")
    }
  }

  implicit val executionReportFormat: RootJsonFormat[ExecutionReport] = new RootJsonFormat[ExecutionReport] {
    override def write(obj: ExecutionReport): JsValue = ???

    override def read(json: JsValue): ExecutionReport = {
      val fields = json.asJsObject

      ExecutionReport(
        eventTime = fields.fields.get("E").map(_.convertTo[Long]).getOrElse(0L),
        eventType = fields.fields.get("e").map(_.convertTo[String]).getOrElse(""),
        symbol = fields.fields.get("s").map(s => TradingSymbol(s.convertTo[String])).getOrElse(new TradingSymbol("")),
        clientOrderId = fields.fields.get("c").map(_.convertTo[String]).getOrElse(""),
        side = fields.fields.get("S").map(s => Side(s.convertTo[String])).getOrElse(Side.UNKNOWN),
        orderType = fields.fields.get("o").map(_.convertTo[String]).getOrElse(""),
        timeInForce = fields.fields.get("f").map(_.convertTo[String]).getOrElse(""),
        orderQuantity = fields.fields.get("q").map(_.convertTo[BigDecimal](bigDecimalFormat)).getOrElse(BigDecimal(0)),
        orderPrice = fields.fields.get("p").map(_.convertTo[BigDecimal](bigDecimalFormat)).getOrElse(BigDecimal(0)),
        stopPrice = fields.fields.get("P").map(_.convertTo[BigDecimal](bigDecimalFormat)).getOrElse(BigDecimal(0)),
        icebergQty = fields.fields.get("F").map(_.convertTo[BigDecimal](bigDecimalFormat)).getOrElse(BigDecimal(0)),
        orderId = fields.fields.get("i").map(_.convertTo[Long]).getOrElse(0L),
        orderStatus = fields.fields.get("X").map(s => OrderStatus(s.convertTo[String])).getOrElse(OrderStatus.UNKNOWN),
        orderRejectReason = fields.fields.get("r").map(_.convertTo[String]).getOrElse(""),
        orderReportType = fields.fields.get("x").map(_.convertTo[String]).getOrElse(""),
        lastExecutedQty = fields.fields.get("l").map(_.convertTo[BigDecimal](bigDecimalFormat)).getOrElse(BigDecimal(0)),
        cumulativeFilledQty = fields.fields.get("z").map(_.convertTo[BigDecimal](bigDecimalFormat)).getOrElse(BigDecimal(0)),
        lastExecutedPrice = fields.fields.get("L").map(_.convertTo[BigDecimal](bigDecimalFormat)).getOrElse(BigDecimal(0)),
        commissionAmount = fields.fields.get("n").map(_.convertTo[BigDecimal](bigDecimalFormat)).getOrElse(BigDecimal(0)),
        commissionAsset = fields.fields.get("N").map(_.convertTo[String]),
          transactionTime = fields.fields.get("T").map(_.convertTo[Long]).getOrElse(0L),
        tradeId = fields.fields.get("t").map(_.convertTo[Long]).getOrElse(0L)
      )
    }
  }
}
