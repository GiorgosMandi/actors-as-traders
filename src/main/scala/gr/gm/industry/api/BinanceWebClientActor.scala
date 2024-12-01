package gr.gm.industry.api

import akka.actor
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, Uri}
import akka.http.scaladsl.unmarshalling.Unmarshal
import gr.gm.industry.model.dao.Order.Order
import gr.gm.industry.model.dao.CoinPrice
import gr.gm.industry.model.dao.CoinPrice.PriceError
import gr.gm.industry.model.dto.PriceDto
import gr.gm.industry.utils.Constants.{Coin, Currency}

import java.util.UUID
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Random, Success}

object BinanceWebClientActor {

  implicit val actorSystem: actor.ActorSystem = akka.actor.ActorSystem()
  implicit val dispatcher: ExecutionContextExecutor = actorSystem.dispatcher

  val BINANCE_URL = "https://www.binance.com/api/v3/ticker"

  // supported messages
  sealed trait BinanceMessage

  // requests
  sealed trait BinanceRequest extends BinanceMessage

  case class OrderRequest(order: Order, replyTo: ActorRef[BinanceMessage]) extends BinanceRequest

  case class PriceRequest(coin: Coin, currency: Currency, replyTo: ActorRef[BinanceMessage]) extends BinanceRequest

  case class RepetitivePriceRequest(coin: Coin, currency: Currency, replyTo: ActorRef[BinanceMessage], delay: Int) extends BinanceRequest

  // responses
  sealed trait BinanceResponse extends BinanceMessage

  case class OrderRequestResponse(order: Order, replyTo: ActorRef[BinanceMessage]) extends BinanceResponse

  case class OrderFulfilled(id: UUID) extends BinanceResponse

  case class OrderFailed(id: UUID) extends BinanceResponse

  case class PriceResponse(price: CoinPrice) extends BinanceResponse

  case class PriceRequestResponse(price: CoinPrice, replyTo: ActorRef[BinanceMessage]) extends BinanceResponse

  case class OrderRequestErrorResponse(error: String, id: UUID, replyTo: ActorRef[BinanceMessage]) extends BinanceResponse

  case class PriceRequestErrorResponse(error: PriceError, replyTo: ActorRef[BinanceMessage]) extends BinanceResponse


  private def fetchPrice(coin: Coin, currency: Currency): Future[Either[PriceError, CoinPrice]] = {
    val priceUri = Uri(s"$BINANCE_URL/price")
      .withQuery(Query("symbol" -> s"${coin.name}${currency.name}"))
    Http()
      .singleRequest(HttpRequest(method = HttpMethods.GET, uri = priceUri))
      .flatMap(response => Unmarshal(response).to[PriceDto])
      .map(priceDto => CoinPrice(priceDto))
  }

  private def placeOrder(order: Order)
                        (implicit dispatcher: ExecutionContextExecutor):
  Future[Order] = Future {
    // todo
    Thread.sleep(Random.nextInt(8) * 1000)
    order
  }

  def handleRequest(request: BinanceRequest,
                    context: ActorContext[BinanceMessage],
                    timers: TimerScheduler[BinanceMessage]
                   ): Behavior[BinanceMessage] = {
    request match {
      case OrderRequest(order, replyTo) =>
        val orderF = placeOrder(order)(dispatcher)
        context.pipeToSelf(orderF) {
          case Success(order) =>
            OrderRequestResponse(order, replyTo)
          case Failure(reason) =>
            OrderRequestErrorResponse(reason.toString, order.id, replyTo)
        }
        Behaviors.same

      // fetch price and pipe response to self
      case PriceRequest(coin, currency, replyTo) =>
        val futurePrice = fetchPrice(coin, currency)
        context.pipeToSelf(futurePrice) {
          case Success(either) =>
            either match {
              case Left(error) => PriceRequestErrorResponse(error, replyTo)
              case Right(price) => PriceRequestResponse(price, replyTo)
            }
        }
        Behaviors.same

      // send multiple price requests
      case RepetitivePriceRequest(coin, currency, replyTo, delay) =>
        context.log.warn(
          "Setting up repetitive requests for {} every {}s",
          coin.name, delay
        )
        val priceRequest = PriceRequest(coin, currency, replyTo)
        timers.startTimerWithFixedDelay(priceRequest, FiniteDuration(delay, TimeUnit.SECONDS))
        Behaviors.same
    }
  }

  def handleResponse(response: BinanceResponse, context: ActorContext[BinanceMessage]):
  Behavior[BinanceMessage] = {
    response match {
      case OrderRequestResponse(order, replyTo) =>
        replyTo ! OrderFulfilled(order.id)
        Behaviors.same

      case OrderRequestErrorResponse(msg, orderId, replyTo) =>
        context.log.warn(s"Order failed with message: $msg")
        // replyTo ! OrderFailed(orderId)
        Behaviors.same

      case PriceRequestResponse(price, replyTo) =>
        context.log.warn(s"Received $price.")
        // replyTo ! PriceResponse(price)
        Behaviors.same

      case PriceRequestErrorResponse(error, replyTo) =>
        context.log.warn(s"Received a price error: ${error.message}")
        Behaviors.same
    }
  }

  def apply(): Behavior[BinanceMessage] = Behaviors.withTimers { timers =>
    Behaviors.receive { (context, message) =>
      message match {
        case request: BinanceRequest =>
          handleRequest(request, context, timers)
        case response: BinanceResponse =>
          handleResponse(response, context)
      }
    }
  }
}
