package gr.gm.industry

import akka.NotUsed
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorSystem, Behavior, DispatcherSelector}
import akka.stream.Materializer
import akka.util.Timeout
import com.typesafe.config.{Config, ConfigFactory}
import gr.gm.industry.gecko.CoinGeckoListenerActor
import gr.gm.industry.gecko.CoinGeckoMessages.Start
import gr.gm.industry.strategies.RandomStrategy
import gr.gm.industry.streams.BinanceStreamingProcessingGraph
import gr.gm.industry.utils.enums.{Coin, Currency}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import gr.gm.industry.utils.enums.Network

object App extends App {

  val conf: Config = ConfigFactory.load()
  private val binanceApiKey: String = conf.getString("app.binance.apiKey")
  private val binanceSecretKey: String = conf.getString("app.binance.secretKey")
  private val net = Network.Testnet
  implicit val timeout: Timeout = 60.second


  private def testBinanceWebSocketSource(): Behavior[NotUsed] = {
    Behaviors.setup { context =>
      implicit val system: ActorSystem[Nothing] = context.system
      implicit val ec: ExecutionContext = context.executionContext
      implicit val mat: Materializer = Materializer(context)
      implicit val ac: ActorContext[NotUsed] = context

      val graph = BinanceStreamingProcessingGraph(
        Coin.BTC,
        Currency.USDT,
        net,
        binanceApiKey,
        binanceSecretKey
      )
      graph.run()
      Behaviors.ignore
    }
  }

  val selectedBehavior = testBinanceWebSocketSource()

  val system = ActorSystem(selectedBehavior, "actors-as-traders", conf)
  implicit val ec: ExecutionContext =
    system.dispatchers.lookup(DispatcherSelector.default())

  val timeoutScheduler = 65.second
  system.scheduler.scheduleOnce(timeoutScheduler, () => system.terminate())
}
