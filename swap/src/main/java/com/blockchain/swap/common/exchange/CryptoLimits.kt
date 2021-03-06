package com.blockchain.swap.common.exchange

import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Money.Companion.max
import info.blockchain.balance.Money.Companion.min

class CryptoLimits(
    private val minCryptoValue: CryptoValue,
    private val maxCryptoValue: CryptoValue
) {

    /**
     * Ensures that the [value] is inside the range [minCryptoValue]..[maxCryptoValue].
     * - If the range is reversed ([minCryptoValue] higher than [maxCryptoValue]), then it will return [CryptoValue.zero].
     * - If the value is below the [minCryptoValue] it will return the [minCryptoValue].
     * - If the value is above the [maxCryptoValue] it will return the [maxCryptoValue].
     * - Otherwise, the value is in the range, and it will return the [value].
     */
    fun clamp(value: CryptoValue) =
        when {
            minCryptoValue > maxCryptoValue -> CryptoValue.zero(value.currency)
            else -> min(max(value, minCryptoValue), maxCryptoValue)
        }
}
