package gr.gm.industry.api

import akka.actor
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, Uri}
import akka.http.scaladsl.unmarshalling.Unmarshal
import gr.gm.industry.model.dao.Order.Order
import gr.gm.industry.model.dao.PriceDao
import gr.gm.industry.model.dao.PriceDao.PriceError
import gr.gm.industry.model.dto.PriceDto
import gr.gm.industry.utils.Constants.{Coin, Currency}

import java.util.UUID
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Random, Success, Try}

object BinanceWebClientActor {

  implicit val actorSystem: actor.ActorSystem = akka.actor.ActorSystem()
  implicit val dispatcher: ExecutionContextExecutor = actorSystem.dispatcher

  val BINANCE_URL = "https://www.binance.com/api/v3/ticker"

  sealed trait WebClientResponse

  case object ACK extends WebClientResponse

  case class OrderFulfilled(id: UUID) extends WebClientResponse

  case class OrderFailed(id: UUID) extends WebClientResponse

  case class PriceResponse(price: PriceDao) extends WebClientResponse

  sealed trait WebClientRequest

  case class OrderRequest(order: Order, replyTo: ActorRef[WebClientResponse]) extends WebClientRequest

  case class OrderRequestResponse(order: Order, replyTo: ActorRef[WebClientResponse]) extends WebClientRequest

  case class OrderRequestErrorResponse(error: String, id: UUID, replyTo: ActorRef[WebClientResponse]) extends WebClientRequest

  case class PriceRequest(coin: Coin, currency: Currency, replyTo: ActorRef[WebClientResponse]) extends WebClientRequest

  case class PriceRequestResponse(price: PriceDao, replyTo: ActorRef[WebClientResponse]) extends WebClientRequest

  case class PriceRequestErrorResponse(error: PriceError, replyTo: ActorRef[WebClientResponse]) extends WebClientRequest


  def fetchPrice(coin: Coin, currency: Currency): Future[Either[PriceError, PriceDao]] = {
    val priceUri = Uri(s"$BINANCE_URL/price")
      .withQuery(Query("symbol" -> s"${coin.name}${currency.name}"))
    val request: HttpRequest = HttpRequest(method = HttpMethods.GET, uri = priceUri)
    Http()
      .singleRequest(request)
      .flatMap(response => Unmarshal(response).to[PriceDto])
      .map(priceDto => PriceDao(priceDto))
  }

  def placeOrder(order: Order)(implicit dispatcher: ExecutionContextExecutor): Future[Order] = {
    Future {
      Thread.sleep(Random.nextInt(8) * 1000)
      order
    }
  }

  def apply(): Behavior[WebClientRequest] =
    Behaviors.setup { context =>

      def handleFutureOrder(orderId: UUID,
                            replyTo: ActorRef[WebClientResponse]
                           )(tOrder: Try[Order]): WebClientRequest = {
        tOrder match {
          case Success(order) =>
            OrderRequestResponse(order, replyTo)
          case Failure(reason) =>
            context.log.error(s"Order failed: $reason")
            OrderRequestErrorResponse(reason.toString, orderId, replyTo)
        }
      }

      def handlingRequests(): Behavior[WebClientRequest] = Behaviors.receiveMessage {
        case OrderRequest(order, replyTo) =>
          val futureOrder = placeOrder(order)(dispatcher)
          context.pipeToSelf(futureOrder)(handleFutureOrder(order.id, replyTo))
          Behaviors.same

        case OrderRequestResponse(order, replyTo) =>
          replyTo ! OrderFulfilled(order.id)
          Behaviors.same

        case OrderRequestErrorResponse(msg, orderId, replyTo) =>
          context.log.warn(s"Order failed with message: $msg")
          replyTo ! OrderFailed(orderId)
          Behaviors.same

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

        case PriceRequestResponse(price, replyTo) =>
          replyTo ! PriceResponse(price)
          Behaviors.same

        case PriceRequestErrorResponse(error, replyTo) =>
          context.log.warn(s"Received a price error: ${error.message}")
          Behaviors.same
      }
      handlingRequests()
    }
}
