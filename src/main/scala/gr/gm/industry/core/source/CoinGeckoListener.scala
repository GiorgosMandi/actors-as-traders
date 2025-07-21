package gr.gm.industry.core.source

import akka.actor
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{Behaviors, TimerScheduler}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.Uri.{Path, Query}
import akka.http.scaladsl.model._
import gr.gm.industry.core.deciders.DecisionMaker
import gr.gm.industry.model.dao.CoinGeckoCoinDto
import gr.gm.industry.utils.exception.CustomException
import gr.gm.industry.utils.jsonProtocols.CoinGeckoCoinDtoProtocol._
import spray.json._

import java.util.concurrent.TimeUnit
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.language.postfixOps
import scala.util.{Failure, Success}

object CoinGeckoListener {

  // system
  implicit val actorSystem: actor.ActorSystem = akka.actor.ActorSystem()
  implicit val dispatcher: ExecutionContextExecutor = actorSystem.dispatcher

  // actions
  sealed trait CoinGeckoAction

  case object ListenerKey extends CoinGeckoAction

  case class Start(delay: Int) extends CoinGeckoAction

  case object Stop extends CoinGeckoAction

  case object Hit extends CoinGeckoAction

  case object Pause extends CoinGeckoAction

  case class Error(message: String) extends CoinGeckoAction

  // request of fetching prices
  def fetchPrice(cryptoId: String = "ethereum", currency: String = "EUR"): Future[CoinGeckoCoinDto] = {
    println("================ Fetching Price By CoinGecko ========================")
    val uri = Uri("https://api.coingecko.com")
      .withPath(Path("/api/v3/simple/price"))
      .withQuery(Query(
        "ids" -> cryptoId,
        "vs_currencies" -> currency,
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

  // Upon start, repetitive price requests will be sent based on the provided delay
  def apply(decisionMaker: DecisionMaker): Behavior[CoinGeckoAction] = Behaviors.withTimers {
    timers: TimerScheduler[CoinGeckoAction] =>
      Behaviors.receive { (context, action) =>
        action match {

          case Start(delay) =>
            context.log.warn("Bootstrapping")
            timers.startTimerWithFixedDelay(ListenerKey, Hit, FiniteDuration(delay, TimeUnit.SECONDS))
            Behaviors.same

          case Hit =>
            fetchPrice()
              .onComplete {
              case Success(ethInfo) =>
                val price = ethInfo.toPrice
                val decision = decisionMaker.decide(price)
                println(s"For $price was decided to $decision.")
              case Failure(exc) =>
                println(s"Failed to receive price, reason: ${exc.toString}")
            }
            Behaviors.same

          case Pause =>
            context.log.warn("I am stopping timer")
            timers.cancel(ListenerKey)
            Behaviors.same

          case Stop =>
            context.log.warn("I am stopping")
            context.stop(context.self)
            timers.cancel(ListenerKey)
            Behaviors.same

          case Error(msg) =>
            context.log.warn(s"Received error: $msg")
            context.stop(context.self)
            Behaviors.same
        }
      }
  }
}
