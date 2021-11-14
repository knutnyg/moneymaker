package xyz.nygaard.util

import java.security.Key
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

private val hmacSha256 = "HmacSHA256"

fun createKey(secret: String): Key = SecretKeySpec(secret.toByteArray(), hmacSha256)
fun createSignature(secretKey: Key, data: ByteArray): String {
    val mac = Mac.getInstance(hmacSha256)
    mac.init(secretKey)
    mac.update(data)
    val digest = mac.doFinal()
    return HexFormat.of().formatHex(digest)
}
