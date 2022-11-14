package gr.gm.industry.api

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem}
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, Uri}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.http.scaladsl.{Http, HttpExt}
import gr.gm.industry.model.dao.Order.Order
import gr.gm.industry.model.dao.PriceDao
import gr.gm.industry.model.dao.PriceDao.PriceError
import gr.gm.industry.model.dto.PriceDto
import gr.gm.industry.utils.Constants.{ACK, Coin, Currency}

import java.util.UUID
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Random, Success}

class BinanceWebClient extends Actor with ActorLogging {

    import context.{dispatcher, system}

    case class OrderFulfilled(id: UUID)

    case class OrderFailed(id: UUID, throwable: Throwable)

    case class PriceRequest(coin: Coin, currency: Currency)

    override def receive: Receive = {
        case order: Order =>
            val senderRef = sender()
            senderRef ! ACK
            placeOrder(order, senderRef)
        case PriceRequest(coin, currency) =>
            sender() ! BinanceWebClient.getPrice(coin, currency)
    }


    def placeOrder(order: Order, senderRef: ActorRef)(implicit dispatcher: ExecutionContextExecutor): Order = {
        BinanceWebClient.placeOrder(order)
          .onComplete {
              case Success(_) =>
                  log.info(s"Order ${order.id} was fulfilled")
                  senderRef ! OrderFulfilled(order.id)
              case Failure(err) =>
                  log.warning(s"Order ${order.id} failed ${err.getMessage}")
                  senderRef ! OrderFailed(order.id, err)
          }
        order
    }
}

object BinanceWebClient {
    val BINANCE_URL = "https://www.binance.com/api/v3/ticker"

    def getPrice(coin: Coin, currency: Currency)(implicit system: ActorSystem): Future[Either[PriceError, PriceDao]] = {
        import system.dispatcher
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

    def placeOrder(order: Order)(implicit dispatcher: ExecutionContextExecutor): Future[Boolean] = {
        Future {
            Thread.sleep(Random.nextInt(8) * 1000)
            true
        }
    }
}
