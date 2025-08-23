package gr.gm.industry.actors

import akka.actor
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{Behaviors, TimerScheduler}
import gr.gm.industry.clients.CoinGeckoHttpClient
import gr.gm.industry.messages.CoinGeckoListenerActorMessages._
import gr.gm.industry.model.TradeDecision.OrderIntent
import gr.gm.industry.strategies.Strategy
import gr.gm.industry.utils.enums.Side.{BUY, SELL}

import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Random, Success}

object CoinGeckoListenerActor {

  // system
  implicit val actorSystem: actor.ActorSystem = akka.actor.ActorSystem()
  implicit val dispatcher: ExecutionContextExecutor = actorSystem.dispatcher


  // Upon start, repetitive price requests will be sent based on the provided delay
  def apply(decisionMaker: Strategy): Behavior[CoinGeckoAction] = Behaviors.withTimers {
    timers: TimerScheduler[CoinGeckoAction] =>
      Behaviors.receive { (context, action) =>
        action match {

          case Start(delay) =>
            context.log.warn("Bootstrapping")
            timers.startTimerWithFixedDelay(ListenerKey, Hit, FiniteDuration(delay, TimeUnit.SECONDS))
            Behaviors.same

          case Hit =>
            CoinGeckoHttpClient.fetchPrice()
              .onComplete {
              case Success(ethInfo) =>
                val price = ethInfo.toPrice
                // warning naive strategy, not properly integrated
                val decision =  if (Random.nextInt() % 2 == 0)
                 OrderIntent(price.price, 10, BUY, price.symbol)
                else
                  OrderIntent(price.price, 10, SELL, price.symbol)

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
