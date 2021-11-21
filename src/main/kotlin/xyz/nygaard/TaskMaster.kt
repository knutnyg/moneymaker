package xyz.nygaard

import xyz.nygaard.core.CreateOrderRequest
import xyz.nygaard.io.FiriClient


sealed interface Action
data class ClearOrders(val type: String = "ClearOrders") : Action
data class AddBid(val req: CreateOrderRequest, val type: String = "AddBid") : Action
data class AddAsk(val req: CreateOrderRequest, val type: String = "AddAsk") : Action
data class KeepAsk(val req: CreateOrderRequest, val type: String = "KeepAsk") : Action
data class KeepBid(val req: CreateOrderRequest, val type: String = "KeepBid") : Action

class TaskMaster(
    private val firiClient: FiriClient,
) : ActionRunner {
    override suspend fun run(actions: List<Action>) {
        actions.forEach { handleAction(it) }
    }

    private suspend fun handleAction(action: Action): Action {
        return when (action) {
            is ClearOrders -> handleClearOrder(action)
            is AddBid -> handleAddBid(action)
            is KeepBid -> handleKeepBid(action)
            is AddAsk -> handleAddAsk(action)
            is KeepAsk -> handleKeepAsk(action)
        }
    }

    private suspend fun handleClearOrder(action: ClearOrders): Action {
        firiClient.deleteActiveOrders()
        return action
    }

    private suspend fun handleKeepBid(action: KeepBid): Action {
        return handleAddBid(AddBid(action.req))
    }

    private suspend fun handleKeepAsk(action: KeepAsk): Action {
        return handleAddAsk(AddAsk(action.req))
    }

    private suspend fun handleAddBid(action: AddBid): Action {
        firiClient.placeOrder(action.req)
        return action
    }

    private suspend fun handleAddAsk(action: AddAsk): Action {
        firiClient.placeOrder(action.req)
        return action
    }
}

internal fun merge(a: List<Action>, b: List<Action>): List<Action> {
    val c = a + b
    val clearAction = c.filterIsInstance<ClearOrders>().firstOrNull()
    val d = c.filter { it !is ClearOrders }

    return when (clearAction) {
        null -> c.filter { it !is KeepAsk && it !is KeepBid }
        else -> listOf(clearAction) + d
    }
}

interface ActionRunner {
    suspend fun run(actions: List<Action>)
}
