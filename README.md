# actors-as-traders

Akka-based playground for algorithmic trading experiments: consumes Binance market data, streams it through strategies, and drives Binance testnet order placement/monitoring via typed actors and Akka Streams.

## TO-DOs

- [ ] finalize generic trader, `BinanceOrderActor` must respond with `GenericTrader` messages
- [ ] test the random strategy
- [ ] place all messages into `messages` package
- [ ] implement the monitor order actor - can an actor monitor multiple orders
- [ ] persist the finalized orders into a postgres
- [ ] examine the persistence of prices, utilizing the TimescaleDb plugin
