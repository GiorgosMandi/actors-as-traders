package services

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.FlowShape
import akka.stream.alpakka.csv.scaladsl.{CsvParsing, CsvToMap}
import akka.stream.scaladsl.GraphDSL.Implicits._
import akka.stream.scaladsl.{Broadcast, FileIO, Flow, GraphDSL, Sink, Zip}
import akka.util.ByteString
import utils.TradeActions

import java.nio.file.Paths
import scala.util.Random

object Columns {
    val ID = "id"
    val TIMESTAMP = "TIMESTAMP"
    val PRICE = "VALUE"
}

object PriceFlow extends App {

    import Columns._
    import utils.TradeActions._


    val path: String = "/home/gmandi/Documents/myProjects/High-Frequency-Trading-App/resources/random-prices.csv"
    val delimiter: Byte = '\t'
    val quoteChar: Byte = '\"'
    val escapeChar: Byte = '\\'


    implicit val system: ActorSystem = ActorSystem("PriceListener")

    val file = Paths.get(path)
    val sourceFile = FileIO.fromPath(file)
    val csvParserFlow: Flow[ByteString, List[ByteString], NotUsed] = CsvParsing.lineScanner(delimiter, quoteChar, escapeChar)
    val csvToMapFlow: Flow[List[ByteString], Map[String, String], NotUsed] = CsvToMap.toMapAsStrings()
    val typeConverter: Flow[Map[String, String], Map[String, String], NotUsed] = Flow[Map[String, String]].map(
        csvMap =>
            Map(
                ID -> csvMap.getOrElse(ID, "-1"),
                TIMESTAMP -> csvMap.getOrElse(TIMESTAMP, ""),
                PRICE -> csvMap.getOrElse(PRICE, "0")
            )
    )


    val graphFlow = Flow.fromGraph(GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>

        val broadcast = builder.add(Broadcast[Map[String, String]](2))
        val zip = builder.add(Zip[TradeActions, Map[String, String]])
        val actionDecider = (_: String) => new Random().nextInt(new java.util.Date().hashCode) % 6 match {
            case 1 => BUY
            case 2 => SELL
            case _ => OMIT
        }
        val actionDeciderFlow = builder.add(Flow[Map[String, String]].map(map => actionDecider(map(PRICE))))

        broadcast.out(0) ~> actionDeciderFlow
        actionDeciderFlow ~> zip.in0
        broadcast.out(1) ~> zip.in1

        FlowShape(broadcast.in, zip.out)
    })

    val printer = Sink.foreach[(TradeActions, Map[String, String])](t => println(t))


    sourceFile
      .via(csvParserFlow)
      .via(csvToMapFlow)
      .via(typeConverter)
      .via(Flow[Map[String, String]].take(20))
      .via(graphFlow)
      .to(printer)
      .run()
}
