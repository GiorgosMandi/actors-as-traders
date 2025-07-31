package gr.gm.industry.clients

import akka.actor.ClassicActorSystemProvider
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, HttpResponse, StatusCodes, Uri}
import akka.http.scaladsl.model.Uri.{Path, Query}
import akka.stream.Materializer
import gr.gm.industry.model.dao.CoinGeckoCoinDto
import gr.gm.industry.utils.enums.Currency.EUR
import gr.gm.industry.utils.enums.{Coin, Currency}
import gr.gm.industry.utils.exception.CustomException
import gr.gm.industry.utils.jsonProtocols.CoinGeckoCoinDtoProtocol._
import spray.json._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

object CoinGeckoHttpClient {

  var coinGeckoUri = "https://api.coingecko.com"

  def fetchPrice(coin: Coin = Coin.ETH, currency: Currency = EUR)
                (implicit system: ClassicActorSystemProvider,
                 ec: ExecutionContext,
                 mat: Materializer
                ): Future[CoinGeckoCoinDto] = {
    val cryptoId = this.getCryptoId(coin)
    val uri = Uri(coinGeckoUri)
      .withPath(Path("/api/v3/simple/price"))
      .withQuery(Query(
        "ids" -> cryptoId,
        "vs_currencies" -> currency.name,
        "include_market_cap" -> "true",
        "include_24hr_vol" -> "true",
        "include_24hr_change" -> "true"
      ))
    val request: HttpRequest = HttpRequest(method = HttpMethods.GET, uri = uri)
    Http()
      .singleRequest(request)
      .flatMap {
        case HttpResponse(StatusCodes.OK, _, e, _) =>
          e.toStrict(5.seconds).flatMap { entity =>
            val jsonString = entity.data.utf8String
            val ethInfo = jsonString.parseJson.convertTo[CoinGeckoCoinDto]
            Future.successful(ethInfo)
          }
        case HttpResponse(StatusCodes.Forbidden, _, e, _) =>
          Future.failed(CustomException("Exceeded available requests"))
      }
  }

  private def getCryptoId(coin: Coin): String =
    coin match {
      case Coin.ETH => "ethereum"
      case _ => ""
  }
}
