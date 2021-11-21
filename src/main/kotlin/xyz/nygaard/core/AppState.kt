package xyz.nygaard.core

import io.ktor.application.*
import xyz.nygaard.Action
import xyz.nygaard.io.ActiveOrder
import xyz.nygaard.io.Market
import xyz.nygaard.io.MarketTicker
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

data class ActiveTradesState(
    val activeOrders: List<ActiveOrder>,
    val lastUpdatedAt: Instant = Instant.now(),
)

data class FilledOrdersState(
    val filledOrders: List<ActiveOrder>,
    val lastUpdatedAt: Instant = Instant.now(),
)

data class ActionsState(
    val actions: List<Action>,
    val lastUpdatedAt: Instant = Instant.now(),
)
data class MarketState(
    val markets: Map<Market, MarketTicker>,
    val lastUpdatedAt: Instant = Instant.now(),
)

data class AppState(
    val market: MarketState,
    val activeTrades: ActiveTradesState,
    val filledOrders: FilledOrdersState,
    val prevActionSet: ActionsState,
    val lastUpdatedAt: Instant = Instant.now(),
) {
    companion object {
        private val listeners: ConcurrentHashMap<ApplicationCall, (AppState) -> Unit> = ConcurrentHashMap()

        private val appState: AtomicReference<AppState> = AtomicReference(
            AppState(
                activeTrades = ActiveTradesState(activeOrders = listOf(), lastUpdatedAt = Instant.now()),
                filledOrders = FilledOrdersState(
                    filledOrders = listOf(),
                    lastUpdatedAt = Instant.now(),
                ),
                prevActionSet = ActionsState(listOf()),
                lastUpdatedAt = Instant.now(),
                market = MarketState(markets = mapOf()),
            )
        )

        private fun notify(state: AppState) {
            listeners.forEach { (_, u) -> u(state) }
        }

        fun listen(o: ApplicationCall, l: (AppState) -> Unit) {
            listeners.putIfAbsent(o, l)
        }

        fun removeListener(o: ApplicationCall) {
            listeners.remove(o)
        }

        fun listenersCount() = listeners.size

        fun update(f: (AppState) -> AppState): AppState {
            val now = Instant.now()
            val next = appState.updateAndGet {
                f(it).copy(
                    lastUpdatedAt = now,
                )
            }
            notify(next)
            return next
        }

        fun get(): AppState {
            return appState.get().copy()
        }
    }
}