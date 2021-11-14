package xyz.nygaard.io.requests

import xyz.nygaard.toJsonBytes
import xyz.nygaard.util.createSignature
import java.security.Key
import java.time.Instant

open class RequestBase private constructor(
    val timestamp: String,
    val validity: String,
) {
    constructor(ts: Instant = Instant.now(), validity: String = "2000") : this(ts.epochSecond.toString(), validity)

    fun signWith(clientSecret: Key) =
        createSignature(clientSecret, toJsonBytes(this))
}