import com.fasterxml.jackson.module.kotlin.readValue
import xyz.nygaard.ActiveOrder
import xyz.nygaard.IFiriClient
import xyz.nygaard.objectMapper

class FiriClientMock: IFiriClient {
    override suspend fun getActiveOrders(): List<ActiveOrder> {
        return listOf(
            objectMapper.readValue("""
                [
                  {
                    "id": 2610958,
                    "market": "BTCNOK",
                    "type": "bid",
                    "price": "50.00",
                    "amount": "1.00000000",
                    "remaining": "1.00000000",
                    "matched": "0.00000000",
                    "cancelled": "0.00000000",
                    "created_at": "2020-04-27T17:38:00.776Z"
                  }
                ]
            """.trimIndent())
        )
    }
}