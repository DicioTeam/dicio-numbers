package org.dicio.numbers.lang.it

import org.dicio.numbers.parser.lexer.TokenStream
import org.dicio.numbers.unit.Number
import org.dicio.numbers.unit.isNullOrZero
import org.dicio.numbers.util.NumberExtractorUtils

class ItalianNumberExtractor internal constructor(private val ts: TokenStream) {
    fun numberPreferOrdinal(): Number? {
        val number = numberSuffixMultiplier() // first try with suffix multiplier, e.g. dozzina
            ?: numberSignPoint(true) // then try with normal number

        return if (number == null) {
            null
        } else {
            // a number was found, maybe it has a valid denominator?
            divideByDenominatorIfPossible(number)
        }
    }

    fun numberPreferFraction(): Number? {
        val number = numberSuffixMultiplier() // first try with suffix multiplier, e.g. dozzina
            ?: numberSignPoint(false) // then try without ordinal

        return if (number == null) {
            // maybe an ordinal number?
            numberSignPoint(true)
        } else {
            // a number was found, maybe it has a valid denominator?
            // note that e.g. "a couple halves" ends up here, but that's valid
            divideByDenominatorIfPossible(number)
        }
    }

    fun numberNoOrdinal(): Number? {
        // for now this function is used internally just for duration parsing, but maybe it could
        // be exposed to library users, giving more control over how ordinals are handled.

        val number = numberSuffixMultiplier() // first try with suffix multiplier, e.g. dozzina
            ?: numberSignPoint(false) // then try without ordinal

        return if (number == null) {
            null
        } else {
            // a number was found, maybe it has a valid denominator?
            // note that e.g. "una mezza coppia" ends up here, but that's valid
            divideByDenominatorIfPossible(number)
        }
    }

    fun numberMustBeInteger(): Number? {
        val number = numberSuffixMultiplierInteger() // first try with suffix multiplier, e.g. dozzina
            ?: numberSignInteger(true) // then try with normal number

        return if (number == null) {
            null
        } else {
            // a number was found, maybe it has a valid denominator?
            // note that e.g. "doppia dozzina" ends up here, but that's valid
            val multiplier = numberSuffixMultiplierInteger()
            if (multiplier == null) {
                number
            } else {
                number.multiply(multiplier)
            }
        }
    }


    fun divideByDenominatorIfPossible(numberToEdit: Number): Number {
        // if numberToEdit is directly followed by an ordinal number then it is a fraction (only if numberToEdit is not
        // ordinal or already decimal). Note: a big integer (i.e. 10^24) would be decimal, here we are assuming that
        // such a number will never have a fraction after it for simplicity.

        if (!numberToEdit.isOrdinal && !numberToEdit.isDecimal
            && !ts[0].hasCategory("ignore")
        ) {
            val originalPosition = ts.position
            val denominator = numberInteger(true)
            if (denominator == null) {
                // no denominator found: maybe a custom multiplier? e.g. mezzo (=0.5), dozzina (=12)
                if (ts[0].hasCategory("suffix_multiplier")) {
                    ts.movePositionForwardBy(1)

                    val multiplier = ts[-1].number
                    if (multiplier?.isDecimal == true &&
                        (1 / multiplier.decimalValue()).toLong().toDouble()
                        == (1 / multiplier.decimalValue())
                    ) {
                        // the multiplier is an exact fraction, divide by the denominator converted
                        // to long to possibly preserve the integerness of numberToEdit, e.g.
                        // sedici mezzi should be 8, not 8.0
                        return numberToEdit.divide((1 / multiplier.decimalValue()).toLong())
                    }

                    return numberToEdit.multiply(multiplier)
                }
            } else if (denominator.isOrdinal && denominator.moreThan(2)) {
                return numberToEdit.divide(denominator) // valid denominator, e.g. un quinto
            } else {
                // invalid denominator, e.g. sei primi
                ts.position = originalPosition // restore to original position
            }
        }
        return numberToEdit
    }

    fun numberSuffixMultiplier(): Number? {
        if (ts[0].hasCategory("suffix_multiplier")) {
            ts.movePositionForwardBy(1)
            return ts[-1].number // a suffix multiplier, e.g. dozen, half, score, percent
        } else {
            return null
        }
    }

    fun numberSuffixMultiplierInteger(): Number? {
        if (ts[0].hasCategory("suffix_multiplier") && ts[0].number!!.isInteger) {
            ts.movePositionForwardBy(1)
            return ts[-1].number // a suffix multiplier, e.g. dozen, score
        } else {
            return null
        }
    }

    fun numberSignPoint(allowOrdinal: Boolean): Number? {
        return NumberExtractorUtils.signBeforeNumber(ts) { numberPoint(allowOrdinal) }
    }

    fun numberSignInteger(allowOrdinal: Boolean): Number? {
        return NumberExtractorUtils.signBeforeNumber(ts) { numberInteger(allowOrdinal) }
    }

    fun numberPoint(allowOrdinal: Boolean): Number? {
        var n = numberInteger(allowOrdinal).let { // the `let` makes `n` be of non-null type
            if (it == null || it.isOrdinal) {
                // numbers can not start with just "virgola"
                // no point or fraction separator can appear after an ordinal number
                return@numberPoint it
            }
            it
        }

        if (ts[0].hasCategory("point")) {
            // parse point indicator from e.g. "twenty one point four five three"

            if (!ts[1].hasCategory("digit_after_point")
                && (!NumberExtractorUtils.isRawNumber(ts[1]) || ts[2].hasCategory("ordinal_suffix"))
            ) {
                // also return if next up is an ordinal raw number, i.e. followed by °/esimo
                return n // there is an only comma at the end of the number: it is not part of it
            }
            ts.movePositionForwardBy(1)

            var magnitude = 0.1
            if (ts[0].value.length > 1 && NumberExtractorUtils.isRawNumber(ts[0])) {
                // handle sequence of raw digits after point, e.g. .0123
                // value.length > 1 since multiple single-digits are handled below, e.g. . 0 1 2 3
                for (i in 0 until ts[0].value.length) {
                    n = n.plus((ts[0].value[i].code - '0'.code) * magnitude)
                    magnitude /= 10.0
                }
                ts.movePositionForwardBy(1)
            } else {
                // read as many digits as possible, e.g. point one six 5 one 0 three
                while (true) {
                    if (ts[0].hasCategory("digit_after_point")
                        || (ts[0].value.length == 1 && NumberExtractorUtils.isRawNumber(ts[0])
                                && !ts[1].hasCategory("ordinal_suffix"))
                    ) {
                        // do not allow ordinal raw numbers, i.e. followed by st/nd/rd/th
                        n = n.plus(ts[0].number!!.multiply(magnitude))
                        magnitude /= 10.0
                    } else {
                        break // reached a word that is not a valid digit
                    }
                    ts.movePositionForwardBy(1)
                }
            }
        } else if (ts[0].hasCategory("fraction_separator")) {
            // parse fraction from e.g. "twenty divided by one hundred"

            var separatorLength = 1
            if (ts[1].hasCategory("fraction_separator_secondary")) {
                separatorLength = 2 // also remove "by" after "divided by"
            }

            ts.movePositionForwardBy(separatorLength)
            val denominator = numberInteger(false)
            if (denominator.isNullOrZero()) {
                ts.movePositionForwardBy(-separatorLength) // not a fraction, reset
            } else {
                return n.divide(denominator)
            }
        }

        return n
    }

    fun numberInteger(allowOrdinal: Boolean): Number? {
        var n = NumberExtractorUtils.numberMadeOfGroups(ts) { ts, lastMultiplier ->
            NumberExtractorUtils.numberGroupShortScale(ts, allowOrdinal, lastMultiplier)
        }
        if (n == null) {
            // try to parse big raw numbers (>=1000), e.g. 1207
            return NumberExtractorUtils.numberBigRaw(ts, allowOrdinal)
        } else if (n.isOrdinal) {
            return n // no more checks, as the ordinal word comes last, e.g. million twelfth
        }

        // n != null from here on
        if (n.lessThan(1000)) {
            // parse raw number n separated by comma, e.g. 123,045,006
            // assuming current position is at the first comma
            if (NumberExtractorUtils.isRawNumber(ts[-1]) && ts[0].hasCategory("thousand_separator") && ts[1].value.length == 3 && NumberExtractorUtils.isRawNumber(
                    ts[1]
                )
            ) {
                val originalPosition = ts.position - 1

                while (ts[0].hasCategory("thousand_separator") && ts[1].value.length == 3 && NumberExtractorUtils.isRawNumber(
                        ts[1]
                    )
                ) {
                    n = n!!.multiply(1000).plus(ts[1].number)
                    ts.movePositionForwardBy(2) // do not allow ignored words in between
                }

                if (ts[0].hasCategory("ordinal_suffix")) {
                    if (allowOrdinal) {
                        ts.movePositionForwardBy(1)
                        return n!!.withOrdinal(true) // ordinal number, e.g. 20,056,789th
                    } else {
                        ts.position = originalPosition
                        return null // found ordinal number, revert since allowOrdinal is false
                    }
                }
            }
        }

        return n // e.g. six million, three hundred and twenty seven
    }
}
