package xyz.nygaard.io

import io.ktor.utils.io.errors.*
import io.mockk.*
import org.junit.jupiter.api.Test
import xyz.nygaard.TaskMaster
import xyz.nygaard.core.Ticker

internal class TickerTest {

    private val firiMock = mockk<FiriClient>(relaxed = true)

    private val tick = Ticker(
        firiClient = firiMock,
        taskMaster = mockk(),
        onActiveOrders = mockk(),
        onActions = mockk(),
        onMarket = mockk()
    )

    @Test
    fun `survives failing api calls`() {
        coEvery { firiMock.fetchMarketTicker() } throws IOException("Fail")
        tick.run()
    }
}