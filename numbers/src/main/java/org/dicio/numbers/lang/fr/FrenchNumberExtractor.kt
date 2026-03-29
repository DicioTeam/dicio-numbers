package org.dicio.numbers.lang.fr

import org.dicio.numbers.parser.lexer.TokenStream
import org.dicio.numbers.unit.Number
import org.dicio.numbers.util.NumberExtractorUtils

class FrenchNumberExtractor internal constructor(private val ts: TokenStream) {
    fun numberPreferOrdinal(): Number? {
        val number = numberSuffixMultiplier()
            ?: numberSignPoint(true)

        return if (number == null) {
            null
        } else {
            divideByDenominatorIfPossible(number)
        }
    }

    fun numberPreferFraction(): Number? {
        val number = numberSuffixMultiplier()
            ?: numberSignPoint(false)

        return if (number == null) {
            numberSignPoint(true)
        } else {
            divideByDenominatorIfPossible(number)
        }
    }

    fun numberNoOrdinal(): Number? {
        val number = numberSuffixMultiplier()
            ?: numberSignPoint(false)

        return if (number == null) {
            null
        } else {
            divideByDenominatorIfPossible(number)
        }
    }

    fun numberMustBeInteger(): Number? {
        val number = numberSuffixMultiplierInteger()
            ?: numberSignInteger(true)

        return if (number == null) {
            null
        } else {
            val multiplier = numberSuffixMultiplierInteger()
            if (multiplier == null) {
                number
            } else {
                number.multiply(multiplier)
            }
        }
    }

    fun divideByDenominatorIfPossible(numberToEdit: Number): Number {
        if (!numberToEdit.isOrdinal && !numberToEdit.isDecimal
            && !ts[0].hasCategory("ignore")
        ) {
            val originalPosition = ts.position
            val denominator = numberInteger(true)
            if (denominator == null) {
                if (ts[0].hasCategory("suffix_multiplier")) {
                    ts.movePositionForwardBy(1)
                    val multiplier = ts[-1].number
                    if (multiplier?.isDecimal == true &&
                        (1 / multiplier.decimalValue()).toLong().toDouble()
                        == (1 / multiplier.decimalValue())
                    ) {
                        return numberToEdit.divide((1 / multiplier.decimalValue()).toLong())
                    }
                    if (multiplier != null) {
                        return numberToEdit.multiply(multiplier)
                    }
                }
            } else if (denominator.isOrdinal && denominator.moreThan(2)) {
                return numberToEdit.divide(denominator)
            } else {
                ts.position = originalPosition
            }
        }
        return numberToEdit
    }

    fun numberSuffixMultiplier(): Number? {
        return if (ts[0].hasCategory("suffix_multiplier")) {
            ts.movePositionForwardBy(1)
            ts[-1].number
        } else {
            null
        }
    }

    fun numberSuffixMultiplierInteger(): Number? {
        return if (ts[0].hasCategory("suffix_multiplier") && ts[0].number?.isInteger == true) {
            ts.movePositionForwardBy(1)
            ts[-1].number
        } else {
            null
        }
    }

    fun numberSignPoint(allowOrdinal: Boolean): Number? {
        return NumberExtractorUtils.signBeforeNumber(ts) { numberPoint(allowOrdinal) }
    }

    fun numberSignInteger(allowOrdinal: Boolean): Number? {
        return NumberExtractorUtils.signBeforeNumber(ts) { numberInteger(allowOrdinal) }
    }

    fun numberPoint(allowOrdinal: Boolean): Number? {
        var n = numberInteger(allowOrdinal) ?: return null
        if (n.isOrdinal) {
            return n
        }

        if (ts[0].hasCategory("point")) {
            if (!ts[1].hasCategory("digit_after_point")
                && (!NumberExtractorUtils.isRawNumber(ts[1]) || ts[2].hasCategory("ordinal_suffix"))
            ) {
                return n
            }
            ts.movePositionForwardBy(1)

            var magnitude = 0.1
            if (ts[0].value.length > 1 && NumberExtractorUtils.isRawNumber(ts[0])) {
                for (i in 0 until ts[0].value.length) {
                    n = n.plus((ts[0].value[i].code - '0'.code) * magnitude)
                    magnitude /= 10.0
                }
                ts.movePositionForwardBy(1)
            } else {
                while (true) {
                    if (ts[0].hasCategory("digit_after_point")
                        || (ts[0].value.length == 1 && NumberExtractorUtils.isRawNumber(ts[0])
                                && !ts[1].hasCategory("ordinal_suffix"))
                    ) {
                        val digitVal = ts[0].number
                        if (digitVal != null) {
                            n = n.plus(digitVal.multiply(magnitude))
                        }
                        magnitude /= 10.0
                    } else {
                        break
                    }
                    ts.movePositionForwardBy(1)
                }
            }
        } else if (ts[0].hasCategory("fraction_separator")) {
            var separatorLength = 1
            if (ts[1].hasCategory("fraction_separator_secondary")) {
                separatorLength = 2
            }
            ts.movePositionForwardBy(separatorLength)
            val denominator = numberInteger(false)
            if (denominator == null) {
                ts.movePositionForwardBy(-separatorLength)
            } else {
                n = n.divide(denominator)
            }
        }

        return n
    }

    fun numberInteger(allowOrdinal: Boolean): Number? {
        var n = NumberExtractorUtils.numberMadeOfGroups(ts) { ts, lastMultiplier ->
            numberGroupFr(ts, allowOrdinal, lastMultiplier)
        }
        if (n == null) {
            return NumberExtractorUtils.numberBigRaw(ts, allowOrdinal)
        } else if (n.isOrdinal) {
            return n
        }

        if (n.lessThan(1000)) {
            if (NumberExtractorUtils.isRawNumber(ts[-1])
                && ts[0].hasCategory("thousand_separator")
                && ts[1].value.length == 3
                && NumberExtractorUtils.isRawNumber(ts[1])
            ) {
                val originalPosition = ts.position - 1
                while (ts[0].hasCategory("thousand_separator")
                    && ts[1].value.length == 3
                    && NumberExtractorUtils.isRawNumber(ts[1])
                ) {
                    val groupVal = ts[1].number
                    if (groupVal != null) {
                        n = n!!.multiply(1000).plus(groupVal)
                    }
                    ts.movePositionForwardBy(2)
                }
                if (ts[0].hasCategory("ordinal_suffix")) {
                    if (allowOrdinal) {
                        ts.movePositionForwardBy(1)
                        n = n!!.withOrdinal(true)
                    } else {
                        ts.position = originalPosition
                        return null
                    }
                }
            }
        }

        return n
    }

    /**
     * Custom group parsing for French, handling "soixante + teen" = 70-79
     * and "quatre + vingt(s)" = 80-99 for non-hyphenated input.
     */
    private fun numberGroupFr(
        ts: TokenStream,
        allowOrdinal: Boolean,
        lastMultiplier: Double
    ): Number? {
        if (lastMultiplier < 1000) return null

        val originalPosition = ts.position
        val groupValue = numberLessThan1000Fr(allowOrdinal)
        if (groupValue != null && groupValue.isOrdinal) {
            return groupValue
        }

        val nextNotIgnore = if (groupValue == null) 0
        else ts.indexOfWithoutCategory("ignore", 0)
        val ordinal = ts[nextNotIgnore].hasCategory("ordinal")
        if (ts[nextNotIgnore].hasCategory("multiplier") && (allowOrdinal || !ordinal)) {
            val multiplier = ts[nextNotIgnore].number
            if (multiplier != null && multiplier.lessThan(lastMultiplier)) {
                ts.movePositionForwardBy(nextNotIgnore + 1)
                return if (groupValue == null) {
                    multiplier.withOrdinal(ordinal)
                } else {
                    multiplier.multiply(groupValue).withOrdinal(ordinal)
                }
            }
        } else {
            return groupValue
        }

        ts.position = originalPosition
        return null
    }

    /**
     * Parses a number < 1000 with French-specific handling:
     * - "soixante" (60) + teen (10-19) = 70-79
     * - "quatre" (4) + "vingt/vingts" (20) = 80, optionally + digit/teen = 81-99
     */
    private fun numberLessThan1000Fr(allowOrdinal: Boolean): Number? {
        var hundred: Long = -1
        var ten: Long = -1
        var digit: Long = -1
        var ordinal = false
        var firstIteration = true

        while (true) {
            val nextNotIgnore = if (firstIteration) {
                firstIteration = false
                0
            } else {
                ts.indexOfWithoutCategory("ignore", 0)
            }

            if (!allowOrdinal && ts[nextNotIgnore].hasCategory("ordinal")) break

            when {
                ts[nextNotIgnore].hasCategory("digit") -> {
                    val digitValue = ts[nextNotIgnore].number?.integerValue() ?: break

                    // French special: "quatre" (4) + "vingt(s)" (20) = 80
                    if (digitValue == 4L && ten < 0 && digit < 0 && hundred < 0) {
                        val savedPos = ts.position
                        ts.movePositionForwardBy(nextNotIgnore + 1)
                        val nextIdx2 = ts.indexOfWithoutCategory("ignore", 0)
                        if (ts[nextIdx2].hasCategory("tens")
                            && ts[nextIdx2].number?.integerValue() == 20L
                        ) {
                            ts.movePositionForwardBy(nextIdx2 + 1)
                            // Now check for additional digit or teen (81-99)
                            val nextIdx3 = ts.indexOfWithoutCategory("ignore", 0)
                            val addCat = when {
                                ts[nextIdx3].hasCategory("teen") -> "teen"
                                ts[nextIdx3].hasCategory("digit")
                                        && ts[nextIdx3].isNumberEqualTo(0).not() -> "digit"
                                else -> null
                            }
                            if (addCat != null) {
                                ts.movePositionForwardBy(nextIdx3 + 1)
                                return Number(
                                    80L + (ts[-1].number?.integerValue() ?: 0L),
                                    ts[-1].hasCategory("ordinal") && allowOrdinal
                                )
                            }
                            // Just 80
                            ten = 80
                            digit = 0 // block further digit
                            if (ts[-1].hasCategory("ordinal") && allowOrdinal) {
                                ordinal = true; break
                            }
                            continue
                        }
                        ts.position = savedPos
                    }

                    if (digit < 0 && (ts[nextNotIgnore].isNumberEqualTo(0).not()
                                || (ten < 0 && hundred < 0))
                    ) {
                        digit = digitValue
                    } else break
                }

                ts[nextNotIgnore].hasCategory("teen") -> {
                    if (ten < 0 && digit < 0) {
                        ten = ts[nextNotIgnore].number?.integerValue() ?: break
                        digit = 0
                    } else break
                }

                ts[nextNotIgnore].hasCategory("tens") -> {
                    val tensValue = ts[nextNotIgnore].number?.integerValue() ?: break

                    // French special: "soixante" (60) + teen (10-19) = 70-79
                    if (tensValue == 60L && ten < 0 && digit < 0) {
                        val savedPos = ts.position
                        ts.movePositionForwardBy(nextNotIgnore + 1)
                        val nextIdx2 = ts.indexOfWithoutCategory("ignore", 0)
                        if (ts[nextIdx2].hasCategory("teen")) {
                            val teenVal = ts[nextIdx2].number?.integerValue() ?: -1L
                            if (teenVal >= 10) {
                                ts.movePositionForwardBy(nextIdx2 + 1)
                                ten = 60 + teenVal
                                digit = 0
                                if (ts[-1].hasCategory("ordinal") && allowOrdinal) {
                                    ordinal = true; break
                                }
                                continue
                            }
                        }
                        // Normal soixante = 60
                        ts.position = savedPos
                    }

                    if (ten < 0 && digit < 0) {
                        ten = tensValue
                    } else break
                }

                ts[nextNotIgnore].hasCategory("hundred") -> {
                    if (hundred < 0 && ten < 0) {
                        if (digit < 0) {
                            hundred = 100
                        } else if (digit == 0L) {
                            break
                        } else {
                            hundred = digit * 100
                            digit = -1
                        }
                    } else break
                }

                NumberExtractorUtils.isRawNumber(ts[nextNotIgnore]) -> {
                    val rawNumber = ts[nextNotIgnore].number ?: break
                    if (rawNumber.isDecimal) break

                    if (!allowOrdinal && ts[nextNotIgnore + 1].hasCategory("ordinal_suffix")) break

                    when {
                        rawNumber.lessThan(10) -> {
                            if (digit < 0) digit = rawNumber.integerValue()
                            else break
                        }
                        rawNumber.lessThan(100) -> {
                            if (ten < 0 && digit < 0) {
                                ten = rawNumber.integerValue()
                                digit = 0
                            } else break
                        }
                        rawNumber.lessThan(1000) -> {
                            if (hundred < 0 && ten < 0 && digit < 0) {
                                hundred = rawNumber.integerValue()
                                ten = 0; digit = 0
                            } else break
                        }
                        else -> break
                    }

                    ordinal = ts[nextNotIgnore + 1].hasCategory("ordinal_suffix")
                    if (ordinal) {
                        ts.movePositionForwardBy(nextNotIgnore + 2)
                        break
                    }
                }

                else -> break
            }

            ts.movePositionForwardBy(nextNotIgnore + 1)
            if (ts[-1].hasCategory("ordinal")) {
                ordinal = true; break
            }
        }

        return if (hundred < 0 && ten < 0 && digit < 0) null
        else Number(
            (if (hundred < 0) 0L else hundred)
                    + (if (ten < 0) 0L else ten)
                    + (if (digit < 0) 0L else digit),
            ordinal
        )
    }
}
