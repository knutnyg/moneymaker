package xyz.nygaard.io.responses

import java.math.BigDecimal

data class CurrencyBalance(
    val currency: String,
    val balance: BigDecimal,
    val hold: BigDecimal,
    val available: BigDecimal,
)