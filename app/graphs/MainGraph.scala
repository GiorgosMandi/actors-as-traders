package graphs

import akka.NotUsed
import akka.actor.{ActorSystem, Props}
import akka.stream.alpakka.csv.scaladsl.{CsvParsing, CsvToMap}
import akka.stream.scaladsl.{FileIO, Flow, Sink}
import akka.util.ByteString
import com.typesafe.config.ConfigFactory
import utils.TradeActionT

import java.nio.file.Paths

object MainGraph extends App {

    object Columns {
        val ID = "id"
        val TIMESTAMP = "TIMESTAMP"
        val PRICE = "VALUE"
    }

    val path: String = "/home/gmandi/Documents/myProjects/High-Frequency-Trading-App/resources/random-prices.csv"
    val delimiter: Byte = '\t'
    val quoteChar: Byte = '\"'
    val escapeChar: Byte = '\\'

    val config = ConfigFactory.parseString(
        """
          |akka {
          |     loglevel="INFO"
          |     stdout-loglevel="INFO"
          | }
        """.stripMargin)
    implicit val system: ActorSystem = ActorSystem("PriceListener", ConfigFactory.load(config))

    val trader = system.actorOf(Props[SimpleTrader], "MainTrader")

    val file = Paths.get(path)
    val sourceFile = FileIO.fromPath(file)
    val csvParserFlow: Flow[ByteString, List[ByteString], NotUsed] = CsvParsing.lineScanner(delimiter, quoteChar, escapeChar)
    val csvToMapFlow: Flow[List[ByteString], Map[String, String], NotUsed] = CsvToMap.toMapAsStrings()
    val priceFlow = PriceFlow.priceFlow

    val tradeSink = Sink.foreach[(TradeActionT, Map[String, String])]{case (tradeAction, priceConf) => trader ! (tradeAction, priceConf(Columns.PRICE).toDouble)}

    val printer = Sink.foreach[(TradeActionT, Map[String, String])](t => println(t._1, t._2(Columns.PRICE)))


    sourceFile
      .via(csvParserFlow)
      .via(csvToMapFlow)
      .via(Flow[Map[String, String]].take(1000))
      .via(priceFlow)
      .to(tradeSink)
      .run()

    Thread.sleep(2000)
    trader ! "print"
}
