package graphs.actors.listeners

import akka.actor.{ActorRef, ActorSystem, FSM, Timers}
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCodes}
import akka.http.scaladsl.{Http, HttpExt}
import akka.pattern.pipe
import akka.util.ByteString
import io.circe.parser.parse
import models.CoinGeckoResponse

import scala.language.postfixOps

class CryptoApiListenerFMS(trader: ActorRef) extends FSM[ListenerState, ListenerData] with Timers{

    implicit val system: ActorSystem = context.system
    import context.dispatcher

    val cryptoId = "ethereum"
    val currency = "EUR"

    val requestUrl = s"https://api.coingecko.com/api/v3/simple/price?ids=$cryptoId&vs_currencies=$currency&include_market_cap=true&include_24hr_vol=true&include_24hr_change=true"
    val coinGeckoRequest: HttpRequest = HttpRequest(uri = requestUrl)
    val http: HttpExt = Http(system)

    startWith(Idle, EmptyState)

    when(Idle) {
        case Event(Initialize(delay), EmptyState) =>
            timers.startTimerWithFixedDelay(ListenerKey, Request, delay second)
            goto(Operational)
        case x =>
            log.error(s"Not expecting message ${x.toString}")
            stay()
    }

    when(Operational) {
        case Event(Request, EmptyState) =>
            log.warning("Send Request")
            http.singleRequest(coinGeckoRequest).pipeTo(self)
            stay()

        case Event(HttpResponse(StatusCodes.OK, headers, entity, _), EmptyState) =>
            log.warning("Received Response")
            val futureBody = entity.dataBytes.runFold(ByteString(""))(_ ++ _).map(body => body.utf8String)
            futureBody.map { body: String =>
                parse(body)
                  .map(json => json \\ cryptoId)
                  .map(jsons => jsons.head)
                  .map(json => json.as[CoinGeckoResponse]
                    .foreach(cgResponse => trader ! cgResponse)
                  )
            }
            stay()
        case Event(resp@HttpResponse(code, _, _, _), EmptyState) =>
            log.warning("Request failed, response code: " + code)
            resp.discardEntityBytes()
            stay()

        case Event(Stop, _) =>
            timers.cancel(ListenerKey)
            goto(Idle)
        case x =>
            log.error(s"Not expecting message ${x.toString}")
            stay()
    }
}

object CryptoApiListenerFMS {
    trait ListenerState
    case object Idle extends ListenerState
    case object Operational extends ListenerState

    trait ListenerData
    case object EmptyState extends ListenerData
    case class Initialize(delay: Int) extends ListenerData
    case object Request extends ListenerData
    case object Stop extends ListenerData
}




