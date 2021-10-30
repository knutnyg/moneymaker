package xyz.nygaard.util

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

fun createSignature(key: String, timestamp: String): String {
    val sha256Hmac = Mac.getInstance("HmacSHA256")
    val secretKey = SecretKeySpec(key.toByteArray(), "HmacSHA256")
    sha256Hmac.init(secretKey)
    sha256Hmac.update("""{ 
            "timestamp": "$timestamp",
            "validity": 5000
        }""".toByteArray())
    return sha256Hmac.doFinal().toHex()

    // For base64
    // return Base64.getEncoder().encodeToString(sha256Hmac.doFinal(data.toByteArray()))
}