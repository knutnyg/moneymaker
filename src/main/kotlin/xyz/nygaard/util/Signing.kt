package xyz.nygaard.util

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

fun createSignature(key: String, payloadAsString: String): String {
    val sha256Hmac = Mac.getInstance("HmacSHA256")
    val secretKey = SecretKeySpec(key.toByteArray(), "HmacSHA256")
    sha256Hmac.init(secretKey)
    sha256Hmac.update(payloadAsString.toByteArray())
    return sha256Hmac.doFinal().toHex()
}

fun ByteArray.toHex(): String = joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }