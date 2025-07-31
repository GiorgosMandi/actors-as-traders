package gr.gm.industry.clients

import akka.actor.ClassicActorSystemProvider
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.unmarshalling.Unmarshal
import gr.gm.industry.factories.BinanceUriFactory
import gr.gm.industry.model.dao.Order.{BuyOrder, FailedOrder, Order}
import gr.gm.industry.utils.TradeActions.BUY
import gr.gm.industry.utils.enums.{Coin, Currency}
import spray.json.DefaultJsonProtocol._
import spray.json._

import java.net.URLEncoder
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import scala.concurrent.{ExecutionContext, Future}

class BinanceHttpClient(apiKey: String, secretKey: String)
                       (implicit system: ClassicActorSystemProvider,
                        ec: ExecutionContext
                       ) {
  private val endpoint = "https://testnet.binance.vision/api/v3/order" // Testnet Spot API

  def placeLimitBuy(coin: Coin,
                    currency: Currency,
                    quantity: BigDecimal,
                    price: BigDecimal
                   ): Future[Order] = {
    val timestamp = System.currentTimeMillis()
    val symbol = BinanceUriFactory.getSymbol(coin, currency)
    val params = Map(
      "symbol" -> symbol,
      "side" -> "BUY",
      "type" -> "LIMIT",
      "timeInForce" -> "GTC",
      "quantity" -> quantity.toString(),
      "price" -> price.toString(),
      "recvWindow" -> "5000",
      "timestamp" -> timestamp.toString
    )
    val queryString = params.map { case (k, v) => s"$k=${URLEncoder.encode(v, "UTF-8")}" }.mkString("&")
    val signature = sign(queryString)
    val signedQuery = s"$queryString&signature=$signature"

    val request = HttpRequest(
      method = HttpMethods.POST,
      uri = s"$endpoint?$signedQuery",
      headers = List(RawHeader("X-MBX-APIKEY", apiKey)),
      entity = HttpEntity.Empty // For POST with query params only
    )

    Http().singleRequest(request)
      .flatMap { res =>
        Unmarshal(res.entity).to[String]
          .map { jsString =>
            extractOrderFields(jsString) match {
              case Right((orderId, clientOrderId)) =>
                BuyOrder(orderId, clientOrderId, quantity, coin, price): Order
              case Left(err) =>
                FailedOrder(err, BUY, quantity, coin, price): Order
            }
          }
      }
  }


  private def sign(data: String): String = {
    val sha256HMAC = Mac.getInstance("HmacSHA256")
    val secretKeySpec = new SecretKeySpec(secretKey.getBytes("UTF-8"), "HmacSHA256")
    sha256HMAC.init(secretKeySpec)
    val macData = sha256HMAC.doFinal(data.getBytes("UTF-8"))
    macData.map("%02x".format(_)).mkString
  }

  private def extractOrderFields(jsonStr: String): Either[String, (Long, String)] = {
    try {
      val jsValue = jsonStr.parseJson.asJsObject
      val orderId = jsValue.fields("orderId").convertTo[Long]
      val clientOrderId = jsValue.fields("clientOrderId").convertTo[String]

      Right(orderId, clientOrderId)
    } catch {
      case ex: DeserializationException => Left(s"Failed to extract fields: ${ex.getMessage}")
      case ex: NoSuchElementException => Left(s"Missing expected field: ${ex.getMessage}")
      case ex: Exception => Left(s"Unknown error: ${ex.getMessage}")
    }
  }

}
