package gr.gm.industry.core.traders

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import gr.gm.industry.api.BinanceWebClientActor
import gr.gm.industry.api.BinanceWebClientActor.{OrderFailed, OrderFulfilled, WebClientRequest, WebClientResponse}
import gr.gm.industry.model.dao.Order.{BuyOrder, Order, SellOrder}
import gr.gm.industry.model.dao.PriceDao


// TODO
//  - Pending State DONE
//  - add logs
//  - Deposit in every sell
//  - Deposit command
//  - Print command
//  - Store orders in db after fulfillment -

object NaivePendingTrader {


  trait TraderData

  case object Uninitialized extends TraderData

  case class TransactionState(capital: BigDecimal,
                              activePurchases: Set[BuyOrder],
                              executedOrders: List[Order]
                             ) extends TraderData

  // expected messages
  sealed trait TraderEvent

  case class Initialize(capital: BigDecimal) extends TraderEvent

  case class Buy(priceDao: PriceDao) extends TraderEvent

  case class Sell(priceDao: PriceDao) extends TraderEvent

  case class SellOff(priceDao: PriceDao) extends TraderEvent

  case object Omit extends TraderEvent

  case class OrderResponse(rsp: WebClientResponse) extends TraderEvent

  val MIN_DEPOSIT = 5f
  val BUY_RATIO_TO_CAPITAL = .2f

  private def idle(state: TraderData): Behavior[TraderEvent] = Behaviors.receive {
    (context, event) =>
      (event, state) match {
        case (Initialize(capital), Uninitialized) =>
          context.log.info(s"Trader was initialized with capital: $capital")
          operational(TransactionState(capital, Set[BuyOrder](), Nil))
        case (x, Uninitialized) =>
          context.log.error(s"Not expecting command ${x.toString}")
          Behaviors.same
      }
  }

  private def operational(state: TransactionState): Behavior[TraderEvent] = Behaviors.receive {
    (context, event) =>

      val messageAdapter: ActorRef[WebClientResponse] = context.messageAdapter(rsp => OrderResponse(rsp))
      val brokerActor: ActorRef[WebClientRequest] = context.spawn(BinanceWebClientActor(messageAdapter), "BinanceWebClientActor")

      event match {
        case Buy(priceDao: PriceDao) =>
          context.log.info(s"Received a BUY order: $priceDao")
          val investment = state.capital * BUY_RATIO_TO_CAPITAL
          val capitalAfterOrder = state.capital - investment
          context.log.info(s"Received a BUY order: $priceDao ")
          val buyOrder = BuyOrder(investment, priceDao.coin, priceDao.price)
          brokerActor ! BinanceWebClientActor.OrderRequest(buyOrder)
          val newBudget = TransactionState(capitalAfterOrder, state.activePurchases, state.executedOrders)
          pendingBuying(buyOrder, newBudget)

        case Sell(priceDao: PriceDao) =>
          context.log.info(s"Received a SELL order: $priceDao")
          /**
           * When we SELL, first we find the orders which are profitable if we sell them
           * with the given price. Then we compute the profit, based on the differences
           * of the BUY-ing price and the SELLing price.
           */
          val (profitablePurchases, _) = state.activePurchases
              .partition { order => order.price < priceDao.price }
          val sellingQuantity = profitablePurchases
              .map(order => order.getQuantityInCoins)
              .sum
          val sellOrder = SellOrder(sellingQuantity, priceDao.coin, priceDao.price)
          brokerActor ! BinanceWebClientActor.OrderRequest(sellOrder)
          pendingSelling(sellOrder, priceDao, profitablePurchases, state)
        case SellOff(priceDao: PriceDao) =>
          val sellingQuantity: BigDecimal = state.activePurchases.map(order => order.getQuantityInCoins).sum
          val sellOrder = SellOrder(sellingQuantity, priceDao.coin, priceDao.price)
          brokerActor ! BinanceWebClientActor.OrderRequest(sellOrder)
          pendingSelling(sellOrder, priceDao, state.activePurchases, state)
        case _ =>
          Behaviors.same
      }
  }

  private def pendingBuying(awaitedOrder: BuyOrder, state: TransactionState): Behavior[TraderEvent] =
    Behaviors.receiveMessage {
      case OrderResponse(OrderFulfilled(id)) if id == awaitedOrder.id =>
        val newBudget = TransactionState(state.capital,
          state.activePurchases - id,
          awaitedOrder :: state.executedOrders
        )
        operational(newBudget)
      case OrderResponse(OrderFailed(id)) if id == awaitedOrder.id =>
        operational(state)
      case _ =>
        Behaviors.same
    }

  private def pendingSelling(awaitedOrder: SellOrder,
                             sellingPrice: PriceDao,
                             correspondingOrders: Set[BuyOrder],
                             state: TransactionState
                            ): Behavior[TraderEvent] =
    Behaviors.receiveMessage {
      case OrderResponse(OrderFulfilled(id)) if id == awaitedOrder.id =>
        // compute profit by capitalizing the corresponding orders
        val expectedProfit = correspondingOrders
            .map(order => (sellingPrice.price * order.quantity) - (order.price * order.quantity))
            .sum
        // compute new capital by adding profit
        val capitalAfterOrder = state.capital + expectedProfit
        // remove from active orders the corresponding ones
        val activePurchases = state
            .activePurchases
            .filter(p => correspondingOrders.map(_.id).contains(p.id))
        val newState = TransactionState(capitalAfterOrder, activePurchases, awaitedOrder :: state.executedOrders)
        operational(newState)

      case OrderResponse(OrderFailed(id)) if id == awaitedOrder.id =>
        operational(state)
      case _ =>
        Behaviors.same
    }

  def apply(initialCapital: BigDecimal): Behavior[TraderEvent] =
    idle(TransactionState(initialCapital, Set(), Nil))
}
