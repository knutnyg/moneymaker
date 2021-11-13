package xyz.nygaard.io.requests

class OrderRequest(
    val market: String = "BTCNOK",
    val type: String,
    val price: String,
    val amount: String
): RequestBase()