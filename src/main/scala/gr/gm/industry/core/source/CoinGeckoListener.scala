package gr.gm.industry.core.source

import akka.actor
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{Behaviors, TimerScheduler}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.Uri.{Path, Query}
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import gr.gm.industry.model.dao.CgEthInfo

import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success}


object CoinGeckoListener {

  implicit val actorSystem: actor.ActorSystem = akka.actor.ActorSystem()
  implicit val dispatcher: ExecutionContextExecutor = actorSystem.dispatcher


  sealed trait CoinGeckoAction

  case object ListenerKey extends CoinGeckoAction

  case class Start(delay: Int) extends CoinGeckoAction

  case object Stop extends CoinGeckoAction

  case object Hit extends CoinGeckoAction

  case object Pause extends CoinGeckoAction

  case class Error(message: String) extends CoinGeckoAction

  def parseEntity(entity: ResponseEntity): Unit = {
    Unmarshal(entity).to[CgEthInfo].onComplete {
      case Success(price) =>
        println(s"Successfully fetched price: $price")
      case Failure(ex) =>
        Unmarshal(entity).to[String].onComplete(s =>
          println(s"Failed to parse response to Price: ${ex.getMessage}, $s"))
    }
  }

  def fetchPrice(cryptoId: String = "ethereum", currency: String = "EUR"): Unit = {
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
      .onComplete {
        case Success(response) =>
          response.status match {
            case StatusCodes.OK => parseEntity(response.entity)
            case status => println(s"Request failed with status $status")
          }
        case Failure(ex) =>
          println(s"HTTP request failed: ${ex.getMessage}")
      }
  }

  def apply(): Behavior[CoinGeckoAction] = Behaviors.withTimers {
    timers: TimerScheduler[CoinGeckoAction] =>
      Behaviors.receive { (context, action) =>
        action match {

          case Start(delay) =>
            context.log.warn("Bootstrapping")
            timers.startTimerWithFixedDelay(ListenerKey, Hit, FiniteDuration(delay, TimeUnit.SECONDS))
            Behaviors.same

          case Hit =>
            fetchPrice()
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
