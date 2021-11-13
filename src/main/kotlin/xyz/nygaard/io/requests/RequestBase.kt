package xyz.nygaard.io.requests

import xyz.nygaard.objectMapper
import java.time.Instant

open class RequestBase(
    val timestamp: String = (Instant.now().toEpochMilli() / 1000).toString(),
    val validity: String = "2000"
) {
    fun createSignature(clientSecret: String) =
        xyz.nygaard.util.createSignature(clientSecret, objectMapper.writeValueAsString(this))
}