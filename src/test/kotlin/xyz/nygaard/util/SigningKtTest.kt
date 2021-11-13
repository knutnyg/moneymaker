package xyz.nygaard.util

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import xyz.nygaard.io.requests.RequestBase
import xyz.nygaard.objectMapper

internal class SigningKtTest {

    private val payload = objectMapper.writeValueAsString(RequestBase("1000", "2000"))

    @Test
    fun `verify signing`() {
        // Compared against https://github.com/Herminizer/Firi-API-Signature-Generator
        val signature = createSignature(key = "mykey", payloadAsString = payload)
        assertEquals("53bd9470b7212bd52d7418b534e811661aa86f7899c4a126425606cece46ec5a", signature)
    }
}