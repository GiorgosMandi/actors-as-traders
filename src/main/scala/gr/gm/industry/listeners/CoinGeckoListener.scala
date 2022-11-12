package gr.gm.industry.listeners

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Timers}
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCodes}
import akka.http.scaladsl.{Http, HttpExt}
import akka.pattern.pipe
import akka.util.ByteString
//import io.circe.parser.parse
import gr.gm.industry.dao.CoinGeckoResponse

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

class CoinGeckoListener(trader: ActorRef) extends Actor with ActorLogging with Timers {

    import CoinGeckoListener._
    import context.dispatcher

    implicit val system: ActorSystem = context.system

    val cryptoId = "ethereum"
    val currency = "EUR"

    val requestUrl = s"https://api.coingecko.com/api/v3/simple/price?ids=$cryptoId&vs_currencies=$currency&include_market_cap=true&include_24hr_vol=true&include_24hr_change=true"
    val coinGeckoRequest: HttpRequest = HttpRequest(uri = requestUrl)
    val http: HttpExt = Http(system)

    def receive: Receive = {
        case Start(delay) =>
            log.warning("Bootstrapping")
            timers.startTimerWithFixedDelay(ListenerKey, Hit, delay second)
        case Hit =>
            http.singleRequest(coinGeckoRequest).pipeTo(self)
        case Pause =>
            log.warning("I am stopping timer")
            timers.cancel(ListenerKey)
        case Stop =>
            log.warning("I am stopping")
            context.stop(self)
        case Error(msg) =>
            log.error(s"Received error: $msg")
            context.stop(self)
        case cgResponse: CoinGeckoResponse =>
            log.warning(cgResponse.toString)

        case HttpResponse(StatusCodes.OK, headers, entity, _) =>
            val futureBody = entity.dataBytes.runFold(ByteString(""))(_ ++ _).map(body => body.utf8String)
            futureBody.map { body: String =>
              val cgResponse = CoinGeckoResponse(0.2d, 0.3d, 0.3d, 0.3d)
                trader ! cgResponse
//                parse(body)
//                  .map(json => json \\ cryptoId)
//                  .map(jsons => jsons.head)
//                  .map(json => json.as[CoinGeckoResponse]
//                    .foreach(cgResponse => trader ! cgResponse)
//                  )
            }

        case resp@HttpResponse(code, _, _, _) =>
            log.warning("Request failed, response code: " + code)
            resp.discardEntityBytes()
    }
}

object CoinGeckoListener {
    case object ListenerKey
    case class Start(delay: Int)
    case object Stop
    case object Hit
    case object Pause
    case class Error(message: String)
}
