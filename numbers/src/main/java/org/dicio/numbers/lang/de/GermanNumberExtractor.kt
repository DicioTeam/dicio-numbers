package org.dicio.numbers.lang.de

import org.dicio.numbers.parser.lexer.TokenStream
import org.dicio.numbers.unit.Number
import org.dicio.numbers.util.NumberExtractorUtils

/**
 * German numbers put the units before the tens ("dreiundzwanzig" = three and twenty) and are
 * written as one word up to a million ("zweihundertdreiundvierzigtausend"). The tokenizer splits
 * such compound words into their pieces (zwei, hundert, drei, und, vierzig, tausend), so this
 * class only needs to understand the order of the pieces.
 */
class GermanNumberExtractor internal constructor(private val ts: TokenStream) {
    fun numberPreferOrdinal(): Number? {
        val number = numberSuffixMultiplier() // first try with suffix multiplier, e.g. Dutzend
            ?: numberSignPoint(true) // then try with normal number

        return if (number == null) {
            null
        } else {
            // a number was found, maybe it has a valid denominator?
            divideByDenominatorIfPossible(number)
        }
    }

    fun numberPreferFraction(): Number? {
        val number = numberSuffixMultiplier() // first try with suffix multiplier, e.g. Dutzend
            ?: numberSignPoint(false) // then try without ordinal

        return if (number == null) {
            // maybe an ordinal number?
            numberSignPoint(true)
        } else {
            // a number was found, maybe it has a valid denominator?
            divideByDenominatorIfPossible(number)
        }
    }

    fun numberNoOrdinal(): Number? {
        // for now this function is used internally just for duration parsing, but maybe it could
        // be exposed to library users, giving more control over how ordinals are handled.

        val number = numberSuffixMultiplier() // first try with suffix multiplier, e.g. Dutzend
            ?: numberSignPoint(false) // then try without ordinal

        return if (number == null) {
            null
        } else {
            // a number was found, maybe it has a valid denominator?
            // note that e.g. "eine halbe" ends up here, but that's valid
            divideByDenominatorIfPossible(number)
        }
    }

    fun numberMustBeInteger(): Number? {
        val number = numberSuffixMultiplierInteger() // first try with suffix multiplier, e.g. Dutzend
            ?: numberSignInteger(true) // then try with normal number

        return if (number == null) {
            null
        } else {
            // a number was found, maybe it has a multiplier, e.g. zwei Dutzend
            val multiplier = numberSuffixMultiplierInteger()
            if (multiplier == null) {
                number
            } else {
                number.multiply(multiplier)
            }
        }
    }


    fun divideByDenominatorIfPossible(numberToEdit: Number): Number {
        // if numberToEdit is directly followed by a fraction word (e.g. Viertel) then it is a
        // fraction, only if numberToEdit is not ordinal or already decimal

        if (!numberToEdit.isOrdinal && !numberToEdit.isDecimal
            && !ts[0].hasCategory("ignore")
        ) {
            if (ts[0].hasCategory("fraction_denominator")) {
                // e.g. drei Viertel, ein halb, dreiviertel
                ts.movePositionForwardBy(1)
                return numberToEdit.divide(ts[-1].number!!)
            } else if (ts[0].hasCategory("suffix_multiplier")) {
                // e.g. zwei Dutzend, fünf Prozent
                ts.movePositionForwardBy(1)
                return numberToEdit.multiply(ts[-1].number)
            }
        }
        return numberToEdit
    }

    fun numberSuffixMultiplier(): Number? {
        if (ts[0].hasCategory("suffix_multiplier")) {
            ts.movePositionForwardBy(1)
            return ts[-1].number // a suffix multiplier, e.g. Dutzend, Prozent
        } else if (ts[0].hasCategory("fraction_denominator")) {
            ts.movePositionForwardBy(1)
            return Number(1).divide(ts[-1].number!!) // a fraction on its own, e.g. halbe, Viertel
        } else {
            return null
        }
    }

    fun numberSuffixMultiplierInteger(): Number? {
        if (ts[0].hasCategory("suffix_multiplier") && ts[0].number!!.isInteger) {
            ts.movePositionForwardBy(1)
            return ts[-1].number // a suffix multiplier, e.g. Dutzend
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
        if (ts[0].hasCategory("one_and_a_half")) {
            ts.movePositionForwardBy(1)
            return ts[-1].number // anderthalb
        }

        var n = numberInteger(allowOrdinal).let { // the `let` makes `n` be of non-null type
            if (it == null) {
                // "einhalb" on its own, the "ein" is part of the word
                if (ts[0].hasCategory("plus_half")) {
                    ts.movePositionForwardBy(1)
                    return@numberPoint ts[-1].number
                }
                return@numberPoint null
            } else if (it.isOrdinal) {
                // no point or fraction separator can appear after an ordinal number
                return@numberPoint it
            }
            it
        }

        if (ts[0].hasCategory("plus_half")) {
            // e.g. zweieinhalb (split into zwei + einhalb), eineinhalb
            ts.movePositionForwardBy(1)
            return n.plus(ts[-1].number!!)
        }

        if (ts[0].hasCategory("point")) {
            // parse point indicator from e.g. "einundzwanzig komma vier fünf drei"

            if (!ts[1].hasCategory("digit_after_point")
                && (!NumberExtractorUtils.isRawNumber(ts[1]) || ts[2].hasCategory("ordinal_suffix"))
            ) {
                return n // there is an only comma at the end of the number: it is not part of it
            }
            ts.movePositionForwardBy(1)

            var magnitude = 0.1
            if (ts[0].value.length > 1 && NumberExtractorUtils.isRawNumber(ts[0])) {
                // handle sequence of raw digits after point, e.g. ,0123
                // value.length > 1 since multiple single-digits are handled below, e.g. , 0 1 2 3
                for (i in 0 until ts[0].value.length) {
                    n = n.plus((ts[0].value[i].code - '0'.code) * magnitude)
                    magnitude /= 10.0
                }
                ts.movePositionForwardBy(1)
            } else {
                // read as many digits as possible, e.g. komma eins sechs 5 eins 0 drei
                while (true) {
                    if (ts[0].hasCategory("digit_after_point")
                        || (ts[0].value.length == 1 && NumberExtractorUtils.isRawNumber(ts[0])
                                && !ts[1].hasCategory("ordinal_suffix"))
                    ) {
                        n = n.plus(ts[0].number!!.multiply(magnitude))
                        magnitude /= 10.0
                    } else {
                        break // reached a word that is not a valid digit
                    }
                    ts.movePositionForwardBy(1)
                }
            }
        } else if (ts[0].hasCategory("fraction_separator")) {
            // parse fraction from e.g. "zwanzig geteilt durch hundert"

            var separatorLength = 1
            if (ts[1].hasCategory("fraction_separator_secondary")) {
                separatorLength = 2 // also remove "durch" after "geteilt"
            }

            ts.movePositionForwardBy(separatorLength)
            val denominator = numberInteger(false)
            if (denominator == null) {
                ts.movePositionForwardBy(-separatorLength) // not a fraction, reset
            } else {
                return n.divide(denominator)
            }
        }

        return n
    }

    fun numberInteger(allowOrdinal: Boolean): Number? {
        var n = NumberExtractorUtils.numberMadeOfGroups(ts) { _, lastMultiplier ->
            numberGroup(allowOrdinal, lastMultiplier)
        }
        if (n == null) {
            // try to parse big raw numbers (>=1000), e.g. 1207
            return NumberExtractorUtils.numberBigRaw(ts, allowOrdinal)
        } else if (n.isOrdinal) {
            return n // no more checks, as the ordinal word comes last, e.g. zweitausendste
        }

        if (n.lessThan(1000)) {
            // parse raw number n separated by dots, e.g. 123.045.006
            // assuming current position is at the first dot
            if (NumberExtractorUtils.isRawNumber(ts[-1]) && ts[0].hasCategory("thousand_separator")
                && ts[1].value.length == 3 && NumberExtractorUtils.isRawNumber(ts[1])
            ) {
                val originalPosition = ts.position - 1

                while (ts[0].hasCategory("thousand_separator") && ts[1].value.length == 3
                    && NumberExtractorUtils.isRawNumber(ts[1])
                ) {
                    n = n!!.multiply(1000).plus(ts[1].number)
                    ts.movePositionForwardBy(2) // do not allow ignored words in between
                }

                if (ts[0].hasCategory("ordinal_suffix")) {
                    if (allowOrdinal) {
                        ts.movePositionForwardBy(1)
                        return n!!.withOrdinal(true) // ordinal number, e.g. 20.056.789te
                    } else {
                        ts.position = originalPosition
                        return null // found ordinal number, revert since allowOrdinal is false
                    }
                }
            }
        }

        return n // e.g. sechs Millionen dreihundertsiebenundzwanzig
    }

    /**
     * A number smaller than 1000 optionally followed by a multiplier smaller than the previous one,
     * e.g. "zweihundert tausend" after "drei Millionen".
     */
    fun numberGroup(allowOrdinal: Boolean, lastMultiplier: Double): Number? {
        if (lastMultiplier < 1000) {
            return null // prevent two numbers smaller than 1000 to be one after another
        }

        val originalPosition = ts.position
        val groupValue = numberLessThan1000(allowOrdinal) // e.g. hundertzwölf
        if (groupValue != null && groupValue.isOrdinal) {
            // ordinal numbers can't be followed by a multiplier
            return groupValue
        }

        val nextNotIgnore = if (groupValue == null) 0 else ts.indexOfWithoutCategory("ignore", 0)
        val ordinal = ts[nextNotIgnore].hasCategory("ordinal")
        if (ts[nextNotIgnore].hasCategory("multiplier") && (allowOrdinal || !ordinal)) {
            // prevent ordinal multiplier if allowOrdinal is false
            val multiplier = ts[nextNotIgnore].number!!
            if (multiplier.lessThan(lastMultiplier)) {
                ts.movePositionForwardBy(nextNotIgnore + 1)
                return if (groupValue == null) {
                    // the multiplier alone, e.g. tausend
                    multiplier.withOrdinal(ordinal)
                } else {
                    // number smaller than 1000 followed by a multiplier, e.g. dreizehn Milliarden
                    multiplier.multiply(groupValue).withOrdinal(ordinal)
                }
            }
        } else {
            // no multiplier for this last number group, e.g. hundertzwei
            // also here if the multiplier is ordinal, but allowOrdinal is false
            return groupValue
        }

        // multiplier is too big, reset to previous position
        ts.position = originalPosition
        return null
    }

    /**
     * e.g. "vier hundert drei und zwanzig", "hundert eins", "neunzehn", "dreißigste", "250"
     */
    fun numberLessThan1000(allowOrdinal: Boolean): Number? {
        val originalPosition = ts.position

        if (NumberExtractorUtils.isRawNumber(ts[0]) && !ts[0].number!!.isDecimal
            && ts[0].number!!.moreThan(99) && ts[0].number!!.lessThan(1000)
        ) {
            // raw number with three digits, e.g. 250
            return rawNumberWithOrdinalSuffix(allowOrdinal)
        }

        var hundreds = 0L
        if (ts[0].hasCategory("hundred")) {
            // "hundert" on its own, e.g. hundertzwei
            if (ts[0].hasCategory("ordinal")) {
                return ordinalWordIfAllowed(allowOrdinal, 100, 1)
            }
            hundreds = 100
            ts.movePositionForwardBy(1)
        } else if ((ts[0].hasCategory("digit") || ts[0].hasCategory("teen"))
            && !ts[0].hasCategory("ordinal") && ts[1].hasCategory("hundred")
            && !ts[0].isNumberEqualTo(0) && !ts[0].isNumberEqualTo(10)
        ) {
            // e.g. dreihundert, ein hundert, and neunzehnhundert as used for years
            val value = ts[0].number!!.integerValue() * 100
            if (ts[1].hasCategory("ordinal")) {
                return ordinalWordIfAllowed(allowOrdinal, value, 2) // e.g. dreihundertste
            }
            hundreds = value
            ts.movePositionForwardBy(2)
        }

        if (hundreds > 0 && ts[0].isValue("und") && ts[1].hasCategory("number")) {
            ts.movePositionForwardBy(1) // e.g. hundertundeins
        }

        val lessThan100 = numberLessThan100(allowOrdinal, hundreds > 0)
        if (lessThan100 == null) {
            if (hundreds == 0L) {
                ts.position = originalPosition
                return null
            }
            if (ts[-1].isValue("und")) {
                ts.movePositionForwardBy(-1) // do not consume a dangling "und"
            }
            return Number(hundreds)
        }
        return Number(hundreds + lessThan100.integerValue(), lessThan100.isOrdinal)
    }

    private fun numberLessThan100(allowOrdinal: Boolean, afterHundreds: Boolean): Number? {
        val token = ts[0]
        if (token.hasCategory("ordinal") && !allowOrdinal) {
            return null
        }

        if (token.hasCategory("teen") || token.hasCategory("tens")) {
            // e.g. siebzehn, dreißig, zwanzigste
            ts.movePositionForwardBy(1)
            return token.number!!.withOrdinal(token.hasCategory("ordinal"))
        }

        if (token.hasCategory("digit")) {
            if (afterHundreds && token.isNumberEqualTo(0)) {
                return null // do not allow zero after hundreds, e.g. hundert null
            }

            val tens = ts[2]
            if (!token.hasCategory("ordinal") && !token.isNumberEqualTo(0) && ts[1].isValue("und")
                && tens.hasCategory("tens")
            ) {
                if (tens.hasCategory("ordinal") && !allowOrdinal) {
                    return null // e.g. einundzwanzigste, do not return just "ein"
                }
                // e.g. dreiundzwanzig, einundzwanzigste
                ts.movePositionForwardBy(3)
                return Number(
                    token.number!!.integerValue() + tens.number!!.integerValue(),
                    tens.hasCategory("ordinal")
                )
            }

            // e.g. drei, dritte
            ts.movePositionForwardBy(1)
            return token.number!!.withOrdinal(token.hasCategory("ordinal"))
        }

        if (NumberExtractorUtils.isRawNumber(token) && !token.number!!.isDecimal
            && token.number!!.lessThan(100)
        ) {
            // e.g. 42
            return rawNumberWithOrdinalSuffix(allowOrdinal)
        }

        return null
    }

    private fun rawNumberWithOrdinalSuffix(allowOrdinal: Boolean): Number? {
        val ordinal = ts[1].hasCategory("ordinal_suffix")
        if (ordinal && !allowOrdinal) {
            return null // do not allow ordinal if allowOrdinal is false, e.g. 3te
        }
        ts.movePositionForwardBy(if (ordinal) 2 else 1)
        return ts[if (ordinal) -2 else -1].number!!.withOrdinal(ordinal)
    }

    private fun ordinalWordIfAllowed(allowOrdinal: Boolean, value: Long, tokens: Int): Number? {
        if (!allowOrdinal) {
            return null
        }
        ts.movePositionForwardBy(tokens)
        return Number(value, true)
    }
}
