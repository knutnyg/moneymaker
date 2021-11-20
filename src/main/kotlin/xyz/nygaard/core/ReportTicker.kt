package xyz.nygaard.core

import kotlinx.coroutines.runBlocking
import xyz.nygaard.io.ActiveOrder.Companion.createReport
import xyz.nygaard.io.FiriClient
import java.util.*

class ReportTicker(
    private val firiClient: FiriClient,
) : TimerTask() {
    override fun run() {
        val filledOrders = runBlocking { firiClient.getFilledOrders() }
        filledOrders.createReport()
    }
}