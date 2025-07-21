package gr.gm.industry.streams.sources

import akka.actor.{ActorSystem, ClassicActorSystemProvider}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.{Message, TextMessage, WebSocketRequest}
import akka.{NotUsed, stream}
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Keep, Sink, Source, SourceQueueWithComplete}
import gr.gm.industry.factories.BinanceUriFactory
import gr.gm.industry.utils.enums.{Coin, Currency}

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

object BinanceStreamSource {

  def apply(coin: Coin, currency: Currency)(
    implicit system: ClassicActorSystemProvider,
    ec: ExecutionContext,
    mat: Materializer,
  ): Source[String, NotUsed] = {
    implicit val classicSystem: ActorSystem = system.classicSystem

    val priceWsUri = BinanceUriFactory.getPriceWsUri(coin, currency)
    val webSocketUri = WebSocketRequest(priceWsUri)

    // This queue will push elements from the WebSocket to downstream consumers
    val (queue, source): (SourceQueueWithComplete[String], Source[String, NotUsed]) =
      Source.queue[String](bufferSize = 128, overflowStrategy = stream.OverflowStrategy.dropHead)
        .preMaterialize()

    // Incoming messages from Binance WS
    val incoming = Sink.foreach[Message] {
      case TextMessage.Strict(text) =>
        queue.offer(text)
      case TextMessage.Streamed(stream) =>
        stream.runFold("")(_ + _).foreach(queue.offer)
      case _ =>
    }

    // No messages are sent to Binance
    val outgoing = Source.maybe[Message]

    // Compose the Flow
    val flow = Flow.fromSinkAndSourceMat(incoming, outgoing)(Keep.left)

    // Fire it up
    val (upgradeResponse, closed) = Http().singleWebSocketRequest(webSocketUri, flow)

    // Optionally log upgrade and close status
    upgradeResponse.onComplete {
      case Success(upgrade) if upgrade.response.status.isSuccess() =>
        classicSystem.log.info("WebSocket connected to Binance.")
      case Success(upgrade) =>
        classicSystem.log.warning(s"WebSocket connection failed: ${upgrade.response.status}")
      case Failure(ex) =>
        classicSystem.log.error(s"WebSocket connection failed: ${ex.getMessage}")
    }(ec)

    closed.onComplete {
      case Success(_) => classicSystem.log.info("WebSocket stream closed.")
      case Failure(ex) => classicSystem.log.error(s"WebSocket stream failed: ${ex.getMessage}")
    }(ec)

    source
  }
}
