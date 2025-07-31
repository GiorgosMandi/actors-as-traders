package gr.gm.industry.fms

import akka.actor.{ActorSystem, FSM, Timers}
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCodes}
import akka.http.scaladsl.{Http, HttpExt}
import akka.pattern.pipe
import akka.util.ByteString
import gr.gm.industry.fms.CryptoApiListenerFMS.{ListenerKey, _}
import gr.gm.industry.model.dao.CoinGeckoCoinDto

import java.time.LocalDateTime
import scala.concurrent.duration._
import scala.language.postfixOps

class CryptoApiListenerFMS extends FSM[ListenerState, ListenerData] with Timers {
  import gr.gm.industry.fms.CryptoApiListenerFMS._
    implicit val system: ActorSystem = context.system

    import context.dispatcher

    val cryptoId = "ethereum"
    val currency = "EUR"

    val requestUrl = s"https://api.coingecko.com/api/v3/simple/price?ids=$cryptoId&vs_currencies=$currency&include_market_cap=true&include_24hr_vol=true&include_24hr_change=true"
    val coinGeckoRequest: HttpRequest = HttpRequest(uri = requestUrl)
    val http: HttpExt = Http(system)

    startWith(Idle, Empty)

    when(Idle) {
        case Event(Initialize(delay), Empty) =>
            timers.startTimerWithFixedDelay(ListenerKey, Request, delay second)
            goto(Operational)
        case x =>
            log.error(s"Not expecting message ${x.toString}")
            stay()
    }

    when(Operational) {
        case Event(Request, Empty) =>
            log.warning("Send Request")
            http.singleRequest(coinGeckoRequest).pipeTo(self)
            stay()

        case Event(HttpResponse(StatusCodes.OK, headers, entity, _), Empty) =>
            log.warning("Received Response")
            val futureBody = entity.dataBytes.runFold(ByteString(""))(_ ++ _).map(body => body.utf8String)
            futureBody.map { body: String =>
                val cgResponse = CoinGeckoCoinDto(0.2d, 0.3d, 0.3d, 0.3d, LocalDateTime.now())
//                parse(body)
//                  .map(json => json \\ cryptoId)
//                  .map(jsons => jsons.head)
//                  .map(json => json.as[CoinGeckoResponse]
//                    .foreach(cgResponse => trader ! cgResponse)
//                  )
            }
            stay()
        case Event(resp@HttpResponse(code, _, _, _), Empty) =>
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
    case object ListenerKey

    trait ListenerState
    case object Idle extends ListenerState
    case object Operational extends ListenerState

    trait ListenerData
    case object Empty extends ListenerData

    trait ListenerMessages extends ListenerData
    case class Initialize(delay: Int) extends ListenerMessages
    case object Request extends ListenerMessages
    case object Stop extends ListenerMessages
    case object ACK extends ListenerMessages
}




