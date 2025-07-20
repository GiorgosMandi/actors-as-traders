package gr.gm.industry.actors

import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, Uri}
import akka.http.scaladsl.unmarshalling.Unmarshal
import gr.gm.industry.model.dao.CoinPrice
import gr.gm.industry.model.dao.CoinPrice.PriceError
import gr.gm.industry.model.dto.PriceDto
import gr.gm.industry.utils.Constants.BINANCE_API_URL
import gr.gm.industry.utils.enums.{Coin, Currency}

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.Success

object BinanceWebClientActor {

  // supported messages
  sealed trait BinanceMessage

  sealed trait BinanceRequest extends BinanceMessage
  sealed trait BinanceResponse extends BinanceMessage
  final case class BinancePriceRequest(coin: Coin, currency: Currency, replyTo: ActorRef[BinanceMessage]) extends BinanceRequest
  final case class BinancePricePollingRequest(coin: Coin, currency: Currency, replyTo: ActorRef[BinanceMessage], delay: Int) extends BinanceRequest
  final case class PriceResponse(price: CoinPrice) extends BinanceResponse
  final case class FetchedPrice(price: CoinPrice, replyTo: ActorRef[BinanceMessage]) extends BinanceResponse
  final case class FetchFailed(error: PriceError, replyTo: ActorRef[BinanceMessage]) extends BinanceResponse


  private def fetchPrice(coin: Coin,
                         currency: Currency
                        )
                        (implicit ec: ExecutionContextExecutor,
                         system: ActorSystem[_]
                        ): Future[Either[PriceError, CoinPrice]] = {
    val priceUri = Uri(s"$BINANCE_API_URL/price")
      .withQuery(Query("symbol" -> s"${coin.name}${currency.name}"))
    Http()
      .singleRequest(HttpRequest(method = HttpMethods.GET, uri = priceUri))
      .flatMap {
        case response if response.status.isSuccess() =>
          Unmarshal(response).to[PriceDto].map(dto => CoinPrice(dto))
        case response =>
          response.entity.discardBytes()
          Future.successful(Left(PriceError(s"HTTP error ${response.status}")))
      }
  }


  private def handleRequest(request: BinanceRequest,
                    context: ActorContext[BinanceMessage],
                    timers: TimerScheduler[BinanceMessage]
                   ): Behavior[BinanceMessage] = {
    implicit val ec: ExecutionContextExecutor = context.executionContext
    implicit val system: ActorSystem[Nothing] = context.system
    request match {
      // fetch price and pipe response to self
      case BinancePriceRequest(coin, currency, replyTo) =>
        val futurePrice = fetchPrice(coin, currency)
        context.pipeToSelf(futurePrice) {
          case Success(either) =>
            either match {
              case Left(error) => FetchFailed(error, replyTo)
              case Right(price) => FetchedPrice(price, replyTo)
            }
        }
        Behaviors.same

      // send multiple price requests
      case BinancePricePollingRequest(coin, currency, replyTo, delay) =>
        context.log.warn(
          "Setting up repetitive requests for {} every {}s",
          coin.name, delay
        )
        val priceRequest = BinancePriceRequest(coin, currency, replyTo)
        timers.startTimerWithFixedDelay(priceRequest, FiniteDuration(delay, TimeUnit.SECONDS))
        Behaviors.same
    }
  }

  private def handleResponse(response: BinanceResponse,
                             context: ActorContext[BinanceMessage]
                            ): Behavior[BinanceMessage] = {
    response match {
      case FetchedPrice(price, replyTo) =>
        context.log.warn(s"Received $price.")
        // replyTo ! PriceResponse(price)
        Behaviors.same

      case FetchFailed(error, replyTo) =>
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
