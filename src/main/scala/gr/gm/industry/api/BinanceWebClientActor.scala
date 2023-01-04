package gr.gm.industry.api
import akka.actor
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, Uri}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.http.scaladsl.{Http, HttpExt}
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

  case class OrderRequest(order: Order) extends WebClientRequest

  case class OrderRequestResponse(order: Order) extends WebClientRequest

  case class OrderRequestErrorResponse(error: String, id: UUID) extends WebClientRequest


  case class PriceRequest(coin: Coin, currency: Currency) extends WebClientRequest

  case class PriceRequestResponse(price: PriceDao) extends WebClientRequest

  case class PriceRequestErrorResponse(error: PriceError) extends WebClientRequest


  def fetchPrice(coin: Coin, currency: Currency): Future[Either[PriceError, PriceDao]] = {

    val http: HttpExt = Http()
    val request: HttpRequest = HttpRequest(
      method = HttpMethods.GET,
      uri = Uri(s"$BINANCE_URL/price").withQuery(Query("symbol" -> s"${coin.name}${currency.name}"))
    )
    http
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

  def apply(replyTo: ActorRef[WebClientResponse]): Behavior[WebClientRequest] =
    Behaviors.setup { context =>

        def handleFutureOrder(orderId: UUID)(tOrder: Try[Order]): WebClientRequest = {
          tOrder match {
            case Success(order) =>
              OrderRequestResponse(order)
            case Failure(reason) =>
              context.log.error(s"Order failed: $reason")
              OrderRequestErrorResponse(reason.toString, orderId)
          }
        }

      def handlingRequests(): Behavior[WebClientRequest] = Behaviors.receiveMessage {
        case OrderRequest(order) =>
          val futureOrder = placeOrder(order)(dispatcher)
          context.pipeToSelf(futureOrder) (handleFutureOrder(order.id))
          Behaviors.same

        case OrderRequestResponse(order) =>
            replyTo ! OrderFulfilled(order.id)
            Behaviors.same

        case OrderRequestErrorResponse(msg, orderId) =>
            context.log.warn(s"Order failed with message: $msg")
            replyTo ! OrderFailed(orderId)
            Behaviors.same

        case PriceRequest(coin, currency) =>
          val futurePrice = fetchPrice(coin, currency)
          context.pipeToSelf(futurePrice) {
            case Success(either) =>
              either match {
                case Left(error) => PriceRequestErrorResponse(error)
                case Right(price) => PriceRequestResponse(price)
              }
          }
          Behaviors.same

        case PriceRequestResponse(price) =>
          replyTo ! PriceResponse(price)
          Behaviors.same

        case PriceRequestErrorResponse(error) =>
          context.log.warn(s"Received a price error: ${error.message}")
          Behaviors.same
      }

      handlingRequests()
    }
}
