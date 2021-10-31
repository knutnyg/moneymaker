package xyz.nygaard

import xyz.nygaard.io.ActiveOrder


sealed interface Action
data class ClearOrders(val orderType: ActiveOrder.OrderType) : Action
data class AddBid(val req: CreateOrderRequest) : Action
data class AddAsk(val req: CreateOrderRequest) : Action

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
            is AddAsk -> handleAddAsk(action)
        }
    }

    private suspend fun handleClearOrder(action: ClearOrders): Action {
        firiClient.deleteActiveOrders()
        return action
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

fun merge(a: List<Action>, b: List<Action>): List<Action> {
    val c = a + b
    val clearAction = c.filterIsInstance<ClearOrders>().firstOrNull()
    val d = c.filter { it !is ClearOrders }

    return when (clearAction) {
        null -> c
        else -> listOf(clearAction) + d
    }
}

interface ActionRunner {
    suspend fun run(actions: List<Action>)
}
