package utils

import org.scalatest.flatspec.AnyFlatSpec
import utils.constants.TradeAction
import utils.constants.TradeAction.{BUY, OMIT, SELL}

class TradeActionTest extends AnyFlatSpec {

    "Election" should "elect the major action" in {
        assert(TradeAction.elect((BUY, BUY, BUY, SELL)) == BUY)
        assert(TradeAction.elect((BUY, BUY, BUY, SELL, OMIT, OMIT)) == BUY)
        assert(TradeAction.elect((BUY, BUY, BUY, SELL, OMIT, OMIT, OMIT, OMIT)) == OMIT)
        assert(TradeAction.elect((BUY, BUY, BUY, SELL, SELL, SELL, SELL, OMIT)) == SELL)
    }
}
