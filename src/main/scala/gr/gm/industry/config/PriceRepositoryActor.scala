package gr.gm.industry.config

import akka.actor
import akka.actor.{Actor, ActorLogging, Props}
import akka.pattern.pipe
import akka.stream.Materializer
import akka.util.Timeout
import gr.gm.industry.model.dao.CoinPrice
import reactivemongo.api.bson.{BSONDocument, BSONObjectID}
import reactivemongo.api.bson.collection.BSONCollection
import reactivemongo.api.{AsyncDriver, MongoConnection}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}

class PriceRepositoryActor(config: DbConfig) extends Actor with ActorLogging {

  sealed trait DbMessage
  case class AddPrice(price: CoinPrice) extends DbMessage
  case class GetPrice(id: BSONObjectID) extends DbMessage
  case class UpdatePrice(id: BSONObjectID, price: CoinPrice) extends DbMessage
  case class DeletePrice(id: BSONObjectID) extends DbMessage
  case class PriceAdded(price: CoinPrice)

  case class PriceFetched(price: Option[CoinPrice])
  case class PriceUpdated(result: Boolean)
  case class PriceDeleted(result: Boolean)

  implicit val system: actor.ActorSystem = context.system
  implicit val ec: ExecutionContext = context.dispatcher
  implicit val mat: Materializer = Materializer(context.system)
  implicit val timeout: Timeout = Timeout(5.seconds)

  val uri = config.uri
  val driver = AsyncDriver()

  // Connect to the MongoDB database
  val connection: Future[MongoConnection] = driver.connect(List(uri))
  val db: Future[reactivemongo.api.DB] = connection.flatMap(_.database("mydb"))
  val pricesCollection: Future[BSONCollection] = db.map(_.collection("prices"))


  override def receive: Receive = {
    case AddPrice(price) =>
      pricesCollection.flatMap(_.insert.one(price)).map { result =>
      // todo this if is probably incorrect
        if (result.n > 0) PriceAdded(price)
        else throw new Exception("Insert failed")
      } pipeTo sender()

    case GetPrice(id) =>
      pricesCollection.flatMap { coll =>
        coll.find(BSONDocument("_id" -> id)).one[CoinPrice]
      } pipeTo sender()

    case UpdatePrice(id, price) =>
      val query = BSONDocument("_id" -> id)
      val update = BSONDocument("$set" -> BSONDocument(
        "coin" -> price.coin.toString,
        "currency" -> price.currency.toString,
        "price" -> price.price,
        "timestamp" -> price.timestamp
      ))
      pricesCollection.flatMap { coll =>
        coll.update.one(query, update).map { result =>
          result.n > 0  // True if at least one document was updated
        }
      } pipeTo sender()

    case DeletePrice(id) =>
      val query = BSONDocument("_id" -> id)
      pricesCollection.flatMap(_.delete.one(query)).map { result =>
        result.n > 0  // True if at least one document was deleted
      } pipeTo sender()
  }
}

object PriceRepositoryActor {
  def props: Props = Props[PriceRepositoryActor]
}