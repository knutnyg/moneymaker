import java.math.BigDecimal
import java.math.RoundingMode

fun Double.round(decimals: Int = 2) = BigDecimal(this).setScale(decimals, RoundingMode.HALF_UP).toDouble()