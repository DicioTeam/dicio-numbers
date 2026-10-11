package org.dicio.numbers.lang.sv

import org.dicio.numbers.parser.lexer.TokenStream
import org.dicio.numbers.unit.Number
import org.dicio.numbers.util.NumberExtractorUtils

class SwedishNumberExtractor internal constructor(
    private val ts: TokenStream,
    private val shortScale: Boolean
) {
    fun numberPreferOrdinal(): Number? {
        var number = numberSuffixMultiplier()
        if (number == null) {
            number = numberSignPoint(true)
        }

        return divideByDenominatorIfPossible(number)
    }

    fun numberPreferFraction(): Number? {
        var number = numberSuffixMultiplier()
        if (number == null) {
            number = numberSignPoint(false)
        }

        number = divideByDenominatorIfPossible(number)

        if (number == null) {
            number = numberSignPoint(true)
        }
        return number
    }

    fun numberNoOrdinal(): Number? {
        var number = numberSuffixMultiplier()
        if (number == null) {
            number = numberSignPoint(false)
        }

        number = divideByDenominatorIfPossible(number)

        return number
    }


    fun numberMustBeInteger(): Number? {
        val number = numberSuffixMultiplierInteger()
            ?: NumberExtractorUtils.signBeforeNumber(ts) { numberInteger(true) }

        return if (number == null) {
            null
        } else {
            val multiplier = numberSuffixMultiplierInteger()
            if (multiplier == null) number else number.multiply(multiplier)
        }
    }


    fun divideByDenominatorIfPossible(numberToEdit: Number?): Number? {
        if (numberToEdit == null) {
            if (ts[0].isValue("en") || ts[0].isValue("ett")) {
                val originalPosition = ts.position
                ts.movePositionForwardBy(1)

                val denominator = numberInteger(true)
                if (denominator != null && denominator.isOrdinal && denominator.moreThan(2)) {
                    return Number(1).divide(denominator)
                } else {
                    ts.position = originalPosition
                }
            }
            return null
        }

        if (!numberToEdit.isOrdinal && !numberToEdit.isDecimal
            && !ts[0].hasCategory("ignore")
        ) {
            val originalPosition = ts.position
            val denominator = numberInteger(true)
            if (denominator == null) {
                if (ts[0].hasCategory("suffix_multiplier")) {
                    ts.movePositionForwardBy(1)

                    val multiplier = ts[-1].number
                    if (multiplier!!.isDecimal && (1 / multiplier.decimalValue()).toLong()
                            .toDouble()
                        == (1 / multiplier.decimalValue())
                    ) {
                        return numberToEdit.divide((1 / multiplier.decimalValue()).toLong())
                    }

                    return numberToEdit.multiply(multiplier)
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
        if (ts[0].hasCategory("suffix_multiplier")) {
            ts.movePositionForwardBy(1)
            return ts[-1].number
        } else if ((ts[0].isValue("en") || ts[0].isValue("ett")) && ts[1].hasCategory("suffix_multiplier")) {
            ts.movePositionForwardBy(2)
            return ts[-1].number
        } else {
            return null
        }
    }

    fun numberSuffixMultiplierInteger(): Number? {
        val offset = if ((ts[0].isValue("en") || ts[0].isValue("ett"))
            && ts[1].hasCategory("suffix_multiplier")) 1 else 0
        if (ts[offset].hasCategory("suffix_multiplier") && ts[offset].number!!.isInteger) {
            ts.movePositionForwardBy(offset + 1)
            return ts[-1].number
        }
        return null
    }

    fun numberSignPoint(allowOrdinal: Boolean): Number? {
        return NumberExtractorUtils.signBeforeNumber(ts) { numberPoint(allowOrdinal) }
    }

    fun numberPoint(allowOrdinal: Boolean): Number? {
        var n = numberInteger(allowOrdinal)
        if (n != null && n.isOrdinal) {
            return n
        }

        if (ts[0].hasCategory("point")) {
            if (!ts[1].hasCategory("digit_after_point")
                && (!NumberExtractorUtils.isRawNumber(ts[1]) || ts[2].hasCategory("ordinal_suffix"))
            ) {
                return n
            }

            ts.movePositionForwardBy(1)
            if (n == null) {
                n = Number(0.0)
            }

            var magnitude = 0.1
            if (ts[0].value.length > 1 && NumberExtractorUtils.isRawNumber(ts[0])) {
                for (i in 0 until ts[0].value.length) {
                    n = n!!.plus((ts[0].value[i].code - '0'.code) * magnitude)
                    magnitude /= 10.0
                }
                ts.movePositionForwardBy(1)
            } else {
                while (true) {
                    if (ts[0].hasCategory("digit_after_point")
                        || (ts[0].value.length == 1 && NumberExtractorUtils.isRawNumber(
                            ts[0]
                        )
                                && !ts[1].hasCategory("ordinal_suffix"))
                    ) {
                        n = n!!.plus(ts[0].number!!.multiply(magnitude))
                        magnitude /= 10.0
                    } else {
                        break
                    }
                    ts.movePositionForwardBy(1)
                }
            }
        } else if (n != null && ts[0].hasCategory("fraction_separator")) {
            val originalPosition = ts.position
            ts.movePositionForwardBy(1)
            if (ts[0].hasCategory("fraction_separator_secondary")) {
                ts.movePositionForwardBy(1)
            }

            val denominator = numberInteger(false)
            if (denominator == null || (denominator.isInteger && denominator.integerValue() == 0L)
                || (denominator.isDecimal && denominator.decimalValue() == 0.0)
            ) {
                ts.position = originalPosition
            } else {
                return n.divide(denominator)
            }
        }

        return n
    }

    fun numberInteger(allowOrdinal: Boolean): Number? {
        if (ts[0].hasCategory("ignore")
            && (!ts[0].isValue("en") && !ts[0].isValue("ett") || ts[1].hasCategory("ignore"))
        ) {
            return null
        }

        // Sweden uses long scale
        var n = NumberExtractorUtils.numberMadeOfGroups(ts) { ts, lastMultiplier ->
            numberGroupLongScale(ts, allowOrdinal, lastMultiplier)
        }
        if (n == null) {
            return NumberExtractorUtils.numberBigRaw(ts, allowOrdinal)
        } else if (n.isOrdinal) {
            return n
        }

        if (n.lessThan(100)) {
            val nextNotIgnore = ts.indexOfWithoutCategory("ignore", 0)
            if (ts[nextNotIgnore].hasCategory("hundred")) {
                val ordinal = ts[nextNotIgnore].hasCategory("ordinal")
                if (allowOrdinal || !ordinal) {
                    ts.movePositionForwardBy(nextNotIgnore + 1)
                    return n.multiply(100).withOrdinal(ordinal)
                }
            }
        }

        if (n.lessThan(1000)) {
            if (NumberExtractorUtils.isRawNumber(ts[-1]) && ts[0].hasCategory("thousand_separator") &&
                ts[1].value.length == 3 && NumberExtractorUtils.isRawNumber(ts[1])
            ) {
                val originalPosition = ts.position - 1

                while (ts[0].hasCategory("thousand_separator") && ts[1].value.length == 3 &&
                    NumberExtractorUtils.isRawNumber(ts[1])
                ) {
                    n = n!!.multiply(1000).plus(ts[1].number)
                    ts.movePositionForwardBy(2)
                }

                if (ts[0].hasCategory("ordinal_suffix")) {
                    if (allowOrdinal) {
                        ts.movePositionForwardBy(1)
                        return n!!.withOrdinal(true)
                    } else {
                        ts.position = originalPosition
                        return null
                    }
                }
            }
        }

        return n
    }

    companion object {
        @JvmStatic
        fun numberGroupLongScale(
            ts: TokenStream,
            allowOrdinal: Boolean,
            lastMultiplier: Double
        ): Number? {
            if (lastMultiplier < 1000000) {
                return null
            }

            val originalPosition = ts.position
            var first = NumberExtractorUtils.numberGroupShortScale(ts, allowOrdinal, 1000000.0)
            if (first == null) {
                first = NumberExtractorUtils.numberLessThan1000(ts, allowOrdinal)
                if (first != null && first.isOrdinal) {
                    return first
                }

                if (first == null) {
                    val nextNotIgnore = ts.indexOfWithoutCategory("ignore", 0)
                    if (NumberExtractorUtils.isRawNumber(ts[nextNotIgnore])
                        && ts[nextNotIgnore].number!!.lessThan(1000000)
                    ) {
                        val ordinal = ts[nextNotIgnore + 1].hasCategory("ordinal_suffix")
                        if (ordinal) {
                            if (!allowOrdinal) {
                                return null
                            }
                            ts.movePositionForwardBy(nextNotIgnore + 2)
                            return ts[-2].number!!.withOrdinal(true)
                        }
                        ts.movePositionForwardBy(nextNotIgnore + 1)
                        first = ts[-1].number
                    }
                }
            } else {
                if (first.isOrdinal || first.lessThan(1000)) {
                    return first
                }

                val second = NumberExtractorUtils.numberLessThan1000(ts, allowOrdinal)
                if (second != null) {
                    first = first.plus(second)
                    if (second.isOrdinal) {
                        return first.withOrdinal(true)
                    }
                }
            }

            val nextNotIgnore = ts.indexOfWithoutCategory("ignore", 0)
            val ordinal = ts[nextNotIgnore].hasCategory("ordinal")
            if (ts[nextNotIgnore].hasCategory("multiplier") && (allowOrdinal || !ordinal)
                && ts[nextNotIgnore].number!!.moreThan(1000)
            ) {
                val multiplier = shortMultiplierToLongScale(ts[nextNotIgnore].number)
                if (multiplier!!.lessThan(lastMultiplier)) {
                    ts.movePositionForwardBy(nextNotIgnore + 1)
                    return if (first == null) {
                        multiplier.withOrdinal(ordinal)
                    } else {
                        multiplier.multiply(first).withOrdinal(ordinal)
                    }
                }
            } else {
                return first
            }

            ts.position = originalPosition
            return null
        }

        fun shortMultiplierToLongScale(shortScaleMultiplier: Number?): Number? {
            return if (shortScaleMultiplier!!.integerValue() == 1000000000L) {
                Number(1000000000000L) // miljard
            } else if (shortScaleMultiplier.integerValue() == 1000000000000L) {
                Number(1000000000000000000L) // biljon
            } else if (shortScaleMultiplier.integerValue() == 1000000000000000L) {
                Number(1e24) // biljard
            } else if (shortScaleMultiplier.integerValue() == 1000000000000000000L) {
                Number(1e30) // triljon
            } else {
                shortScaleMultiplier // miljon
            }
        }
    }
}
