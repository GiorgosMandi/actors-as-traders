package gr.gm.industry.listeners

import akka.actor.{ActorRef, ActorSystem, FSM, Timers}
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCodes}
import akka.http.scaladsl.{Http, HttpExt}
import akka.pattern.pipe
import akka.util.ByteString
import gr.gm.industry.listeners.CoinGeckoListener.ListenerKey
import gr.gm.industry.listeners.CryptoApiListenerFMS._
//import io.circe.parser.parse
import gr.gm.industry.dao.CoinGeckoResponse

import scala.concurrent.duration._
import scala.language.postfixOps

class CryptoApiListenerFMS(trader: ActorRef) extends FSM[ListenerState, ListenerData] with Timers{

    implicit val system: ActorSystem = context.system
    import context.dispatcher

    val cryptoId = "ethereum"
    val currency = "EUR"

    val requestUrl = s"https://api.coingecko.com/api/v3/simple/price?ids=$cryptoId&vs_currencies=$currency&include_market_cap=true&include_24hr_vol=true&include_24hr_change=true"
    val coinGeckoRequest: HttpRequest = HttpRequest(uri = requestUrl)
    val http: HttpExt = Http(system)

    startWith(Idle, Unitialize)

    when(Idle) {
        case Event(Initialize(delay), Unitialize) =>
            goto(Operational) using Initialize(delay)
        case _ =>
            log.error("Not expecting message")
            stay()
    }

    when(Operational) {
        case Event(Initialize(delay), _) =>
            timers.startTimerWithFixedDelay(ListenerKey, Request, delay second)
            stay()
        case Event(Request, _) =>
            http.singleRequest(coinGeckoRequest).pipeTo(self)
            stay()

        case Event(HttpResponse(StatusCodes.OK, headers, entity, _), _) =>
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
            stay()
        case Event(resp@HttpResponse(code, _, _, _), _) =>
            log.warning("Request failed, response code: " + code)
            resp.discardEntityBytes()
            stay()

        case Event(Stop, _) =>
           goto(Idle)
    }

}
object CryptoApiListenerFMS {
    trait ListenerState
    case object Idle extends ListenerState
    case object Operational extends ListenerState

    trait ListenerData
    case object Unitialize extends ListenerData
    case class Initialize(delay: Int) extends ListenerData
    case object Request extends ListenerData
    case object Stop extends ListenerData


}




