package xyz.nygaard.core

import io.ktor.application.*
import xyz.nygaard.Action
import xyz.nygaard.ActiveTradesState
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

data class AppState(
    val activeTrades: ActiveTradesState,
    val prevActionSet: List<Action>,
    val lastUpdatedAt: Instant,
) {
    companion object {
        private val listeners: ConcurrentHashMap<ApplicationCall, (AppState) -> Unit> = ConcurrentHashMap()

        private val appState: AtomicReference<AppState> = AtomicReference(
            AppState(
                activeTrades = ActiveTradesState(activeOrders = listOf()),
                prevActionSet = listOf(),
                lastUpdatedAt = Instant.now(),
            )
        )

        internal fun notify(state: AppState) {
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