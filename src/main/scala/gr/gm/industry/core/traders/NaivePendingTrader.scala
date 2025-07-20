package gr.gm.industry.core.traders

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import gr.gm.industry.actors.BinanceWebClientActor.BinanceMessage
import gr.gm.industry.model.dao.CoinPrice
import gr.gm.industry.model.dao.Order.{BuyOrder, Order, SellOrder}


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
                              brokerActor: ActorRef[BinanceMessage]
                             ) extends TraderData

  // expected messages
  sealed trait TradeActionEvent

  final case class Initialize(capital: BigDecimal) extends TradeActionEvent
  final case class Buy(priceDao: CoinPrice) extends TradeActionEvent
  final case class Sell(priceDao: CoinPrice) extends TradeActionEvent
  final case class SellOff(priceDao: CoinPrice) extends TradeActionEvent
  final case object Omit extends TradeActionEvent
  final case class OrderResponse(rsp: BinanceMessage) extends TradeActionEvent

  val MIN_DEPOSIT = 5f
  val BUY_RATIO_TO_CAPITAL = .2f

  private def idle(state: TraderData): Behavior[TradeActionEvent] = Behaviors.receive {
    (context, event) =>
      val logger = context.log
      (event, state) match {
        case (Initialize(capital), Uninitialized) =>
          logger.error(s"Trader is in Idle mode with capital: $capital")
          Behaviors.same
        case (x, Uninitialized) =>
          logger.error(s"Not expecting command ${x.toString}")
          Behaviors.same
      }
  }

  private def operational(state: TransactionState): Behavior[TradeActionEvent] = Behaviors.receive {
    (context, event) =>

      val logger = context.log

      val messageAdapter: ActorRef[BinanceMessage] = context.messageAdapter(rsp => OrderResponse(rsp))

      event match {
        case Buy(priceDao: CoinPrice) =>
          logger.info(s"Received a BUY order: $priceDao")
          val investment = state.capital * BUY_RATIO_TO_CAPITAL
          val buyOrder = BuyOrder(investment, priceDao.coin, priceDao.price)
          // todo - place BUY order
          pendingBuying(buyOrder, state)

        case Sell(priceDao: CoinPrice) =>
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
            // todo - place SELL order
            pendingSelling(sellOrder, priceDao, profitablePurchases, state)
          }
          else
            Behaviors.same

        case SellOff(priceDao: CoinPrice) =>
            val sellingQuantity: BigDecimal = state.activePurchases.map(order => order.getQuantityInCoins).sum
            val sellOrder = SellOrder(sellingQuantity, priceDao.coin, priceDao.price)
          // todo - place SELL order
            pendingSelling(sellOrder, priceDao, state.activePurchases, state)

        case _ =>
          Behaviors.same
      }
  }

  private def pendingBuying(awaitedOrder: BuyOrder, state: TransactionState): Behavior[TradeActionEvent] =
    Behaviors.receive { (context, event) =>

      val logger = context.log
      logger.info(s"Placing BUY orders is not implemented yet")
      operational(state)
//      event match {
//        case OrderResponse(OrderFulfilled(id)) if id == awaitedOrder.id =>
//          val capitalAfterOrder = state.capital - awaitedOrder.quantity
//          val newState = TransactionState(capitalAfterOrder,
//            state.activePurchases - awaitedOrder,
//            awaitedOrder :: state.executedOrders,
//            state.brokerActor
//          )
//          logger.info(s"BUY Completed: new Capital $capitalAfterOrder")
//          operational(newState)
//        case OrderResponse(OrderFailed(id)) if id == awaitedOrder.id =>
//          operational(state)
//        case _ =>
//          Behaviors.same
//      }
    }

  // todo: log messages
  private def pendingSelling(awaitedOrder: SellOrder,
                             sellingPrice: CoinPrice,
                             correspondingOrders: Set[BuyOrder],
                             state: TransactionState
                            ): Behavior[TradeActionEvent] =
    Behaviors.receive { (context, event) =>

      val logger = context.log
      logger.info(s"Placing SELL orders is not implemented yet")
      operational(state)
//      event match {
//        case OrderResponse(OrderFulfilled(id)) if id == awaitedOrder.id =>
//          // compute profit by capitalizing the corresponding orders
//          val expectedProfit = correspondingOrders
//              .map(order => (sellingPrice.price * order.quantity) - (order.price * order.quantity))
//              .sum
//          // compute new capital by adding profit
//          val capitalAfterOrder = state.capital + expectedProfit
//          // remove from active orders the corresponding ones
//          val activePurchases = state
//              .activePurchases
//              .filter(p => correspondingOrders.map(_.id).contains(p.id))
//          val orders = awaitedOrder :: state.executedOrders
//          val newState = TransactionState(capitalAfterOrder, activePurchases, orders, state.brokerActor)
//          logger.info(s"SELL Completed: new Capital $capitalAfterOrder")
//          operational(newState)
//
//        case OrderResponse(OrderFailed(id)) if id == awaitedOrder.id =>
//          operational(state)
//        case _ =>
//          Behaviors.same
//      }
    }

  def apply(initialCapital: BigDecimal, broker: ActorRef[BinanceMessage]): Behavior[TradeActionEvent] =
    operational(TransactionState(initialCapital, Set[BuyOrder](), Nil, broker))
}
