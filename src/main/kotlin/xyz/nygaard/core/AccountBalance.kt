package xyz.nygaard.core

import CurrencyBalance
import xyz.nygaard.io.responses.Currency

data class AccountBalance(val currencies: Map<Currency, CurrencyBalance>)