package gr.gm.industry.clients

import akka.actor.ClassicActorSystemProvider
import akka.actor.typed.ActorRef
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.ws.{Message, TextMessage, WebSocketRequest}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import gr.gm.industry.messages.OrderEvents.{OrderEvent, WsEvent}
import gr.gm.industry.model.ExecutionReport
import gr.gm.industry.model.TradeDecision.OrderIntent
import gr.gm.industry.model.orders.submitted.{
  FailedPlacedOrder,
  PlacedOrder,
  PlacedOrderTrait
}
import gr.gm.industry.protocol.ExecutionReportProtocol._
import gr.gm.industry.utils.enums.TimeInForce
import gr.gm.industry.utils.enums.TimeInForce.FOK
import spray.json._

import java.net.URLEncoder
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import scala.concurrent.{ExecutionContext, Future}
import gr.gm.industry.utils.enums.Network

/** Lightweight Binance REST/WebSocket client for placing orders and consuming
  * user-data streams.
  *
  * @param apiKey
  *   Binance API key used for signed and key-authenticated endpoints.
  * @param secretKey
  *   Secret key for HMAC signatures on signed requests.
  * @param net
  *   Binance network
  * @param system
  *   Akka classic system provider.
  * @param ec
  *   Execution context for async operations.
  */
class BinanceHttpClient(
    apiKey: String,
    secretKey: String,
    net: Network
)(implicit
    system: ClassicActorSystemProvider,
    ec: ExecutionContext
) {

  private val endpoints = BinanceEndpointProvider.endpoints(net)

  /** Places a signed LIMIT BUY order.
    *
    * @param orderIntent
    *   intent containing symbol, price, quantity, and side.
    * @param timeInForce
    *   Binance TIF flag (defaults to FOK).
    * @return
    *   placed order or a failed placeholder.
    */
  def placeLimitBuy(
      orderIntent: OrderIntent,
      timeInForce: TimeInForce = FOK
  ): Future[PlacedOrderTrait] = {
    val timestamp = System.currentTimeMillis()
    val params = Map(
      "symbol" -> orderIntent.symbol.toString,
      "side" -> "BUY",
      "type" -> "LIMIT",
      "timeInForce" -> FOK.name,
      "quantity" -> orderIntent.quantity.toString(),
      "price" -> orderIntent.price.toString(),
      "recvWindow" -> "5000",
      "timestamp" -> timestamp.toString
    )
    val queryString = params
      .map { case (k, v) => s"$k=${URLEncoder.encode(v, "UTF-8")}" }
      .mkString("&")
    val signature = sign(queryString)
    val signedQuery = s"$queryString&signature=$signature"

    val request = HttpRequest(
      method = HttpMethods.POST,
      uri = s"${endpoints.orderHttp}?$signedQuery",
      headers = List(RawHeader("X-MBX-APIKEY", apiKey)),
      entity = HttpEntity.Empty
    )

    Http()
      .singleRequest(request)
      .flatMap { res =>
        Unmarshal(res.entity)
          .to[String]
          .map { jsString =>
            extractOrderFields(jsString) match {
              case Right((orderId, clientOrderId)) =>
                PlacedOrder(
                  orderIntent,
                  orderId,
                  clientOrderId,
                  timeInForce
                ): PlacedOrderTrait
              case Left(err) =>
                FailedPlacedOrder(
                  orderIntent,
                  s"Failed to extract order fields: $err"
                ): PlacedOrderTrait
            }
          }
      }
  }

  /** Places a signed LIMIT SELL order.
    *
    * @param orderIntent
    *   intent containing symbol, price, quantity, and side.
    * @param timeInForce
    *   Binance TIF flag (defaults to FOK).
    * @return
    *   placed order or a failed placeholder.
    */
  def placeLimitSell(
      orderIntent: OrderIntent,
      timeInForce: TimeInForce = FOK
  ): Future[PlacedOrderTrait] = {
    val timestamp = System.currentTimeMillis()
    val params = Map(
      "symbol" -> orderIntent.symbol.toString,
      "side" -> "SELL",
      "type" -> "LIMIT",
      "timeInForce" -> FOK.name,
      "quantity" -> orderIntent.quantity.toString(),
      "price" -> orderIntent.price.toString(),
      "recvWindow" -> "5000",
      "timestamp" -> timestamp.toString
    )
    val queryString = params
      .map { case (k, v) => s"$k=${URLEncoder.encode(v, "UTF-8")}" }
      .mkString("&")
    val signature = sign(queryString)
    val signedQuery = s"$queryString&signature=$signature"

    val request = HttpRequest(
      method = HttpMethods.POST,
      uri = s"${endpoints.orderHttp}?$signedQuery",
      headers = List(RawHeader("X-MBX-APIKEY", apiKey)),
      entity = HttpEntity.Empty
    )

    Http()
      .singleRequest(request)
      .flatMap { res =>
        Unmarshal(res.entity)
          .to[String]
          .map { jsString =>
            extractOrderFields(jsString) match {
              case Right((orderId, clientOrderId)) =>
                PlacedOrder(
                  orderIntent,
                  orderId,
                  clientOrderId,
                  timeInForce
                ): PlacedOrderTrait
              case Left(err) =>
                FailedPlacedOrder(
                  orderIntent,
                  s"Failed to extract order fields: $err"
                ): PlacedOrderTrait
              case other =>
                FailedPlacedOrder(
                  orderIntent,
                  s"Unexpected response: $other"
                ): PlacedOrderTrait
            }
          }
      }
  }

  /** Subscribes to the user-data WebSocket using the provided listenKey and
    * forwards execution reports.
    *
    * @param listenKey
    *   active user-data stream key.
    * @param monitorActor
    *   target actor to receive parsed `WsEvent`s.
    */
  def monitorOrders(
      listenKey: String,
      monitorActor: ActorRef[OrderEvent]
  ): Unit = {
    val wsUrl = endpoints.getBinanceDataStreamWsURI(listenKey)

    val incoming: Sink[Message, _] =
      Sink.foreach {
        case TextMessage.Strict(text) =>
          val report = parseExecutionReport(text)
          monitorActor ! WsEvent(report)

        case streamed: TextMessage.Streamed =>
          streamed.textStream.runFold("")(_ + _).foreach { text =>
            val report = parseExecutionReport(text)
            monitorActor ! WsEvent(report)
          }
        case other =>
          println(s"Ignoring non-text WS message: $other")
      }

    val outgoing: Source[Message, _] = Source.maybe[Message]

    val wsFlow = Flow.fromSinkAndSourceMat(incoming, outgoing)(Keep.left)

    Http().singleWebSocketRequest(WebSocketRequest(wsUrl), wsFlow)
  }

  /** Requests a new listenKey for user-data stream.
    *
    * @return
    *   listenKey string or fails with an error.
    */
  def fetchListenKey(): Future[String] = {
    val request = HttpRequest(
      method = HttpMethods.POST,
      uri = endpoints.userDataStreamHttp,
      headers = List(RawHeader("X-MBX-APIKEY", apiKey))
    )
    Http()
      .singleRequest(request)
      .flatMap { response =>
        response.status match {
          case StatusCodes.OK =>
            Unmarshal(response).to[String].map { body =>
              val jsObj = body.parseJson.asJsObject
              jsObj.fields
                .get("listenKey")
                .map(_.convertTo[String])
                .getOrElse(
                  throw new RuntimeException(
                    s"Missing listenKey in response: $body"
                  )
                )
            }
          case _ =>
            Unmarshal(response).to[String].flatMap { body =>
              Future.failed(
                new RuntimeException(
                  s"Failed to fetch listenKey: ${response.status} $body"
                )
              )
            }
        }
      }
  }

  /** Extends the lifetime of an existing listenKey.
    *
    * @param listenKey
    *   listener key to refresh.
    * @return
    *   success or failed future with API error.
    */
  def keepAliveListenKey(listenKey: String): Future[Unit] = {
    val request = HttpRequest(
      method = HttpMethods.PUT,
      uri = s"${endpoints.userDataStreamHttp}?listenKey=$listenKey",
      headers = List(headers.RawHeader("X-MBX-APIKEY", apiKey))
    )
    Http().singleRequest(request).flatMap { response =>
      response.status match {
        case StatusCodes.OK => Future.successful(())
        case _ =>
          Unmarshal(response).to[String].flatMap { body =>
            Future.failed(
              new RuntimeException(
                s"Failed to refresh listenKey: ${response.status} $body"
              )
            )
          }
      }
    }
  }

  private def sign(data: String): String = {
    val sha256HMAC = Mac.getInstance("HmacSHA256")
    val secretKeySpec =
      new SecretKeySpec(secretKey.getBytes("UTF-8"), "HmacSHA256")
    sha256HMAC.init(secretKeySpec)
    val macData = sha256HMAC.doFinal(data.getBytes("UTF-8"))
    macData.map("%02x".format(_)).mkString
  }

  private def extractOrderFields(
      jsonStr: String
  ): Either[String, (Long, String)] = {
    try {
      val jsValue = jsonStr.parseJson.asJsObject
      val orderId = jsValue.fields("orderId").convertTo[Long]
      val clientOrderId = jsValue.fields("clientOrderId").convertTo[String]

      Right(orderId, clientOrderId)
    } catch {
      case ex: DeserializationException =>
        Left(s"Failed to extract fields: ${ex.getMessage}")
      case ex: NoSuchElementException =>
        Left(s"Missing expected field: ${ex.getMessage}")
      case ex: Exception => Left(s"Unknown error: ${ex.getMessage}")
    }
  }

  /** Parses raw JSON string into an `ExecutionReport`. */
  def parseExecutionReport(json: String): ExecutionReport = {
    json.parseJson.asJsObject.convertTo[ExecutionReport]
  }
}
