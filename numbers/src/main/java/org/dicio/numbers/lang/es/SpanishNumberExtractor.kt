package org.dicio.numbers.lang.es

import org.dicio.numbers.parser.lexer.TokenStream
import org.dicio.numbers.unit.Number
import org.dicio.numbers.unit.isNullOrZero
import org.dicio.numbers.util.NumberExtractorUtils

class SpanishNumberExtractor internal constructor(private val ts: TokenStream) {

    fun numberPreferOrdinal(): Number? {
        // first try with suffix multiplier, e.g. docena (dozen)
        var number = numberSuffixMultiplier()
        if (number == null) {
            number = numberSignPoint(true) // then try with normal number
        }

        // a number was found, maybe it has a valid denominator?
        return divideByDenominatorIfPossible(number)
    }

    fun numberPreferFraction(): Number? {
        // first try with suffix multiplier, e.g. docena (dozen)
        var number = numberSuffixMultiplier()
        if (number == null) {
            number = numberSignPoint(false) // then try without ordinal
        }

        // a number was found, maybe it has a valid denominator?
        number = divideByDenominatorIfPossible(number)

        if (number == null) {
            // maybe an ordinal number?
            number = numberSignPoint(true)
        }
        return number
    }

    fun numberNoOrdinal(): Number? {
        // This function is used internally for duration parsing.
        var number = numberSuffixMultiplier()
        if (number == null) {
            number = numberSignPoint(false)
        }
        return divideByDenominatorIfPossible(number)
    }

    fun divideByDenominatorIfPossible(numberToEdit: Number?): Number? {
        if (numberToEdit == null) {
            // Spanish context: handles "un quinto" (a fifth), where "un" is the numerator.
            if (ts[0].isValue("un") || ts[0].isValue("una")) {
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

        // if numberToEdit is directly followed by an ordinal number then it is a fraction
        if (!numberToEdit.isOrdinal && !numberToEdit.isDecimal && !ts[0].hasCategory("ignore")) {
            val originalPosition = ts.position
            val denominator = numberInteger(true)
            if (denominator == null) {
                // no denominator found: maybe a custom multiplier? e.g. media (=0.5), docena (=12)
                if (ts[0].hasCategory("suffix_multiplier")) {
                    ts.movePositionForwardBy(1)
                    val multiplier = ts[-1].number!!
                    if (multiplier.isDecimal && (1 / multiplier.decimalValue()).toLong().toDouble() == (1 / multiplier.decimalValue())) {
                        return numberToEdit.divide((1 / multiplier.decimalValue()).toLong())
                    }
                    return numberToEdit.multiply(multiplier)
                }
            } else if (denominator.isOrdinal && denominator.moreThan(2)) {
                return numberToEdit.divide(denominator) // valid denominator, e.g. dos tercios
            } else {
                // invalid denominator, e.g. seis primeros
                ts.position = originalPosition // restore to original position
            }
        }
        return numberToEdit
    }

    fun numberSuffixMultiplier(): Number? {
        if (ts[0].hasCategory("suffix_multiplier")) {
            ts.movePositionForwardBy(1)
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
            // no point or fraction separator can appear after an ordinal number
            return n
        }

        if (ts[0].hasCategory("point")) {
            // parse point indicator from e.g. "veintiuno coma cuatro" (twenty one point four)
            if (!ts[1].hasCategory("digit_after_point") && (!NumberExtractorUtils.isRawNumber(ts[1]) || ts[2].hasCategory("ordinal_suffix"))) {
                return n // there is a lone comma at the end of the number: it is not part of it
            }
            ts.movePositionForwardBy(1)
            if (n == null) n = Number(0.0) // numbers can start with just "coma"

            var magnitude = 0.1
            if (ts[0].value.length > 1 && NumberExtractorUtils.isRawNumber(ts[0])) {
                for (i in ts[0].value.indices) {
                    n = n!!.plus((ts[0].value[i].code - '0'.code) * magnitude)
                    magnitude /= 10.0
                }
                ts.movePositionForwardBy(1)
            } else {
                while (true) {
                    if (ts[0].hasCategory("digit_after_point") || (ts[0].value.length == 1 && NumberExtractorUtils.isRawNumber(ts[0]) && !ts[1].hasCategory("ordinal_suffix"))) {
                        n = n!!.plus(ts[0].number!!.multiply(magnitude))
                        magnitude /= 10.0
                    } else {
                        break
                    }
                    ts.movePositionForwardBy(1)
                }
            }
        } else if (n != null && ts[0].hasCategory("fraction_separator")) {
            // parse fraction from e.g. "veinte dividido entre cien"
            val originalPosition = ts.position
            ts.movePositionForwardBy(1)
            if (ts[0].hasCategory("fraction_separator_secondary")) {
                ts.movePositionForwardBy(1)
            }
            val denominator = numberInteger(false)
            if (denominator.isNullOrZero()) {
                ts.position = originalPosition // not a fraction or division by zero, reset
            } else {
                return n.divide(denominator)
            }
        }
        return n
    }

    fun numberInteger(allowOrdinal: Boolean): Number? {
        if (ts[0].hasCategory("ignore")) return null

        var n = NumberExtractorUtils.numberMadeOfGroups(ts, allowOrdinal, NumberExtractorUtils::numberGroupShortScale)
        if (n == null) {
            return NumberExtractorUtils.numberBigRaw(ts, allowOrdinal) // try to parse big raw numbers (>=1000), e.g. 1207
        } else if (n.isOrdinal) {
            return n
        }

        if (n.lessThan(1000)) {
            // parse raw number n separated by comma, e.g. 123.045.006
            if (NumberExtractorUtils.isRawNumber(ts[-1]) && ts[0].hasCategory("thousand_separator") && ts[1].value.length == 3 && NumberExtractorUtils.isRawNumber(ts[1])) {
                val originalPosition = ts.position - 1
                while (ts[0].hasCategory("thousand_separator") && ts[1].value.length == 3 && NumberExtractorUtils.isRawNumber(ts[1])) {
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
}