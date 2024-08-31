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
                              executedOrders: List[Order],
                              brokerActor: ActorRef[WebClientRequest]
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
      val logger = context.log
      (event, state) match {
        case (Initialize(capital), Uninitialized) =>
          logger.error(s"Trader is in Idle mode with capital: ${capital}")
          Behaviors.same
        case (x, Uninitialized) =>
          logger.error(s"Not expecting command ${x.toString}")
          Behaviors.same
      }
  }

  private def operational(state: TransactionState): Behavior[TraderEvent] = Behaviors.receive {
    (context, event) =>

      val logger = context.log

      val messageAdapter: ActorRef[WebClientResponse] = context.messageAdapter(rsp => OrderResponse(rsp))

      event match {
        case Buy(priceDao: PriceDao) =>
          logger.info(s"Received a BUY order: $priceDao")
          val investment = state.capital * BUY_RATIO_TO_CAPITAL
          val buyOrder = BuyOrder(investment, priceDao.coin, priceDao.price)
          state.brokerActor ! BinanceWebClientActor.OrderRequest(buyOrder, messageAdapter)
          pendingBuying(buyOrder, state)

        case Sell(priceDao: PriceDao) =>
          val logger = context.log

          /**
           * When we SELL, first we find the orders which are profitable if we sell them
           * with the given price. Then we compute the profit, based on the differences
           * of the BUY-ing price and the SELLing price.
           */
          val (profitablePurchases, _) = state.activePurchases
              .partition { order => order.price < priceDao.price }
          if (profitablePurchases.nonEmpty){
            logger.info(s"Received a SELL order: $priceDao")
            val sellingQuantity = profitablePurchases
                .map(order => order.getQuantityInCoins)
                .sum
            val sellOrder = SellOrder(sellingQuantity, priceDao.coin, priceDao.price)
            state.brokerActor ! BinanceWebClientActor.OrderRequest(sellOrder, messageAdapter)
            pendingSelling(sellOrder, priceDao, profitablePurchases, state)
          }
          else
            Behaviors.same

        case SellOff(priceDao: PriceDao) =>
            val sellingQuantity: BigDecimal = state.activePurchases.map(order => order.getQuantityInCoins).sum
            val sellOrder = SellOrder(sellingQuantity, priceDao.coin, priceDao.price)
            state.brokerActor ! BinanceWebClientActor.OrderRequest(sellOrder, messageAdapter)
            pendingSelling(sellOrder, priceDao, state.activePurchases, state)

        case _ =>
          Behaviors.same
      }
  }

  private def pendingBuying(awaitedOrder: BuyOrder, state: TransactionState): Behavior[TraderEvent] =
    Behaviors.receive { (context, event) =>

      val logger = context.log
      event match {
        case OrderResponse(OrderFulfilled(id)) if id == awaitedOrder.id =>
          val capitalAfterOrder = state.capital - awaitedOrder.quantity
          val newState = TransactionState(capitalAfterOrder,
            state.activePurchases - awaitedOrder,
            awaitedOrder :: state.executedOrders,
            state.brokerActor
          )
          logger.info(s"BUY Completed: new Capital $capitalAfterOrder")
          operational(newState)
        case OrderResponse(OrderFailed(id)) if id == awaitedOrder.id =>
          operational(state)
        case _ =>
          Behaviors.same
      }
    }

  // todo: log messages
  private def pendingSelling(awaitedOrder: SellOrder,
                             sellingPrice: PriceDao,
                             correspondingOrders: Set[BuyOrder],
                             state: TransactionState
                            ): Behavior[TraderEvent] =
    Behaviors.receive { (context, event) =>

      val logger = context.log
      event match {
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
          val orders = awaitedOrder :: state.executedOrders
          val newState = TransactionState(capitalAfterOrder, activePurchases, orders, state.brokerActor)
          logger.info(s"SELL Completed: new Capital $capitalAfterOrder")
          operational(newState)

        case OrderResponse(OrderFailed(id)) if id == awaitedOrder.id =>
          operational(state)
        case _ =>
          Behaviors.same
      }
    }


  def apply(initialCapital: BigDecimal, broker: ActorRef[WebClientRequest]): Behavior[TraderEvent] =
    operational(TransactionState(initialCapital, Set[BuyOrder](), Nil, broker))
}
