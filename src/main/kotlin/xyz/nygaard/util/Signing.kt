package xyz.nygaard.util

import java.time.LocalDateTime
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

fun createSignature(key: String): String {
    val sha256Hmac = Mac.getInstance("HmacSHA256")
    val secretKey = SecretKeySpec(key.toByteArray(), "HmacSHA256")
    sha256Hmac.init(secretKey)
    return sha256Hmac.doFinal("""{ 
            timestamp: ${LocalDateTime.now()},
            validity: 2000
        }""".toByteArray()).toHex()

    // For base64
    // return Base64.getEncoder().encodeToString(sha256Hmac.doFinal(data.toByteArray()))
}