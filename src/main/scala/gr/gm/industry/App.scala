package gr.gm.industry

import akka.NotUsed
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, DispatcherSelector}
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Sink}
import com.typesafe.config.{Config, ConfigFactory}
import gr.gm.industry.actors.BinanceWebClientActor
import gr.gm.industry.actors.BinanceWebClientActor.{BinanceMessage, BinancePricePollingRequest}
import gr.gm.industry.core.deciders.RandomDecider
import gr.gm.industry.core.flow.PriceFlow
import gr.gm.industry.core.source.{BinancePriceSource, CoinGeckoListener}
import gr.gm.industry.core.traders.NaivePendingTrader
import gr.gm.industry.dto.BookTickerPrice
import gr.gm.industry.sources.BinanceStreamSource
import gr.gm.industry.utils.enums.Coin.ETH
import gr.gm.industry.utils.enums.Currency.EUR
import gr.gm.industry.utils.enums.{Coin, Currency}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import spray.json._
import gr.gm.industry.utils.jsonProtocols.BookTickerPriceProtocol._

object App extends App {

  private def testTradingBehavior(budget: Long = 200): Behavior[NotUsed] = {
    Behaviors.setup { context =>
      implicit val system: ActorSystem[Nothing] = context.system
      val binanceActor: ActorRef[BinanceMessage] = context.spawn(BinanceWebClientActor(), "BinanceActor")
      val priceSource = BinancePriceSource(binanceActor, parallelism = 5, throttle = (4, 1))

      val naiveTrader = context.spawn(NaivePendingTrader(budget, binanceActor), "NaiveTrader")
      val decisionMakerFlow = PriceFlow.decisionMakerFlow
      priceSource.apply()
        .via(decisionMakerFlow)
        .to(Sink.foreach { tradeAction => naiveTrader ! tradeAction })
        .run()

      Behaviors.empty
    }
  }

  private def testBinanceWebSocketSource(): Behavior[NotUsed] = {
    Behaviors.setup { context =>
      implicit val system: ActorSystem[Nothing] = context.system
      implicit val ec: ExecutionContext = context.executionContext
      implicit val mat: Materializer = Materializer(context)
      val priceSource = BinanceStreamSource(Coin.BTC, Currency.USDT)
      val parsePriceFlow: Flow[String, BookTickerPrice, NotUsed] =
      Flow[String].map(_.parseJson.convertTo[BookTickerPrice])
      priceSource
        .via(parsePriceFlow)
        .to(Sink.foreach { price => println(s"Price: $price") })
        .run()
      Behaviors.empty
    }
  }

  def testCoinGeckoBehavior(delay: Int = 2): Behavior[NotUsed] = {
    Behaviors.setup { context =>
      val listener = context.spawn(CoinGeckoListener(RandomDecider), "CoinGeckoListener")
      listener ! CoinGeckoListener.Start(delay)
      Behaviors.empty
    }
  }

  def testBinanceBehavior(delay: Int = 2): Behavior[NotUsed] = {
    Behaviors.setup { context =>
      val binanceWebClientActor = context.spawn(BinanceWebClientActor(), "BinanceWebClientActor")
      binanceWebClientActor ! BinancePricePollingRequest(ETH, EUR, binanceWebClientActor, delay)
      Behaviors.empty
    }
  }

  // TODO Refactor -  create a stream storing prices to Mongo
  val selectedBehavior = testBinanceWebSocketSource()
  val conf: Config = ConfigFactory.load()

  val system = ActorSystem(selectedBehavior, "actors-as-traders", conf)
  implicit val ec: ExecutionContext = system.dispatchers.lookup(DispatcherSelector.default())


  system.scheduler.scheduleOnce(60.second, () => system.terminate())
}
