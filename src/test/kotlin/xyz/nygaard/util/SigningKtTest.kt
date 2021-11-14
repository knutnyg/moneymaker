package xyz.nygaard.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import xyz.nygaard.io.requests.RequestBase
import xyz.nygaard.toJson
import java.time.Instant

internal class SigningKtTest {
    @Test
    fun `verify signing`() {
        // Compared against https://github.com/Herminizer/Firi-API-Signature-Generator
        val key = createKey("mykey")
        val payload = toJson(RequestBase(ts = Instant.ofEpochSecond(1000), validity = "2000"))
        val signature = createSignature(secretKey = key, data = payload.toByteArray())
        assertEquals("53bd9470b7212bd52d7418b534e811661aa86f7899c4a126425606cece46ec5a", signature)
    }
}