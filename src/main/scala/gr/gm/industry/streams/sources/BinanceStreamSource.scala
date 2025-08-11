package gr.gm.industry.streams.sources

import akka.actor.{ActorSystem, ClassicActorSystemProvider}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.{Message, TextMessage, WebSocketRequest}
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Keep, Sink, Source, SourceQueueWithComplete}
import akka.{NotUsed, stream}
import gr.gm.industry.dto.BookTickerPriceDto
import gr.gm.industry.factories.BinanceUriFactory
import gr.gm.industry.utils.jsonProtocols.BookTickerPriceProtocol._
import gr.gm.industry.utils.model.TradingSymbol
import spray.json._

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

object BinanceStreamSource {

  def apply(tradingSymbol: TradingSymbol)(
    implicit system: ClassicActorSystemProvider,
    ec: ExecutionContext,
    mat: Materializer,
  ): Source[BookTickerPriceDto, NotUsed] = {

    implicit val classicSystem: ActorSystem = system.classicSystem
    val priceWsUri = BinanceUriFactory.getPriceWsUri(tradingSymbol)
    val webSocketUri = WebSocketRequest(priceWsUri)

    // This queue will push elements from the WebSocket to downstream consumers
    val (queue, source): (SourceQueueWithComplete[BookTickerPriceDto], Source[BookTickerPriceDto, NotUsed]) =
      Source.queue[BookTickerPriceDto](bufferSize = 128, overflowStrategy = stream.OverflowStrategy.dropHead)
        .preMaterialize()

    // Incoming messages from Binance WS
    val incoming = Sink.foreach[Message] {
      case TextMessage.Strict(text) =>
        val price = text.parseJson.convertTo[BookTickerPriceDto]
        queue.offer(price)
      case TextMessage.Streamed(stream) =>
        stream.runFold("")(_ + _).map(text => text.parseJson.convertTo[BookTickerPriceDto]).foreach(queue.offer)
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
