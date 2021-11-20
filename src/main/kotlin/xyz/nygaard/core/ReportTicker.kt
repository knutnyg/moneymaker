package xyz.nygaard.core

import kotlinx.coroutines.runBlocking
import xyz.nygaard.io.ActiveOrder
import xyz.nygaard.io.ActiveOrder.Companion.createReport
import xyz.nygaard.io.FiriClient
import java.util.*

class ReportTicker(
    private val firiClient: FiriClient,
    private val onFilledOrders: (next: List<ActiveOrder>) -> Unit,
) : TimerTask() {
    override fun run() {
        val filledOrders = runBlocking { firiClient.getFilledOrders() }
        onFilledOrders(filledOrders)
        filledOrders.createReport()
    }
}