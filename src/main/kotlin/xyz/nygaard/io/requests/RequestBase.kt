package xyz.nygaard.io.requests

import xyz.nygaard.objectMapper
import xyz.nygaard.util.createSignature
import java.time.Instant

open class RequestBase(
    val timestamp: String = (Instant.now().toEpochMilli() / 1000).toString(),
    val validity: String = "2000"
) {
    fun createSignature(clientSecret: String) =
        createSignature(clientSecret, objectMapper.writeValueAsString(this))
}