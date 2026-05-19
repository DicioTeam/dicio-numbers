package org.dicio.numbers.lang.en

import org.dicio.numbers.parser.lexer.TokenStream
import org.dicio.numbers.unit.Number
import org.dicio.numbers.unit.isNullOrZero
import org.dicio.numbers.util.NumberExtractorUtils

class EnglishNumberExtractor internal constructor(
    private val ts: TokenStream,
    private val shortScale: Boolean
) {
    fun numberPreferOrdinal(): Number? {
        val number = numberSuffixMultiplier() // first try with suffix multiplier, e.g. dozen
            ?: numberSignPoint(true) // then try with normal number

        // maybe there is a valid denominator? (note: number could be null, e.g. a tenth)
        return divideByDenominatorIfPossible(number)
    }

    fun numberPreferFraction(): Number? {
        val number = numberSuffixMultiplier() // first try with suffix multiplier, e.g. dozen
            ?: numberSignPoint(false) // then try without ordinal

        return if (number == null) {
            // maybe an ordinal number?
            numberSignPoint(true)
        } else {
            // maybe there is a valid denominator? (note: number could be null, e.g. a tenth)
            // note that e.g. "a couple halves" ends up here, but that's valid
            divideByDenominatorIfPossible(number)
        }
    }

    fun numberNoOrdinal(): Number? {
        // for now this function is used internally just for duration parsing, but maybe it could
        // be exposed to library users, giving more control over how ordinals are handled.

        val number = numberSuffixMultiplier() // first try with suffix multiplier, e.g. dozen
            ?: numberSignPoint(false) // then try without ordinal

        // maybe there is a valid denominator? (note: number could be null, e.g. a tenth)
        // note that e.g. "a couple halves" ends up here, but that's valid
        return divideByDenominatorIfPossible(number)
    }

    fun numberMustBeInteger(): Number? {
        val number = numberSuffixMultiplierInteger() // first try with suffix multiplier, e.g. dozen
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


    fun divideByDenominatorIfPossible(numberToEdit: Number?): Number? {
        if (numberToEdit == null) {
            if (ts[0].isValue("a")) {
                // handle cases where
                val originalPosition = ts.position
                ts.movePositionForwardBy(1)

                val denominator = numberInteger(true)
                if (denominator != null && denominator.isOrdinal && denominator.moreThan(2)) {
                    return Number(1).divide(denominator) // valid denominator, e.g. a tenth
                } else {
                    // missing or invalid denominator, e.g. a hello, a four
                    ts.position = originalPosition // restore to original position
                }
            }
            return null
        }

        // if numberToEdit is directly followed by an ordinal number then it is a fraction (only if
        // numberToEdit is not ordinal or already decimal). Note: a big long scale integer (i.e.
        // 10^24) would be decimal, here we are assuming that such a number will never have a
        // fraction after it for simplicity.
        if (!numberToEdit.isOrdinal && !numberToEdit.isDecimal
            && !ts[0].hasCategory("ignore")
        ) {
            val originalPosition = ts.position
            val denominator = numberInteger(true)
            if (denominator == null) {
                // no denominator found: maybe a custom multiplier? e.g. half (=0.5), dozen (=12)
                if (ts[0].hasCategory("suffix_multiplier")) {
                    ts.movePositionForwardBy(1)

                    val multiplier = ts[-1].number
                    if (multiplier!!.isDecimal && (1 / multiplier.decimalValue()).toLong()
                            .toDouble()
                        == (1 / multiplier.decimalValue())
                    ) {
                        // the multiplier is an exact fraction, divide by the denominator converted
                        // to long to possibly preserve the integerness of numberToEdit, e.g.
                        // sixteen quarters should be 4, not 4.0
                        return numberToEdit.divide((1 / multiplier.decimalValue()).toLong())
                    }

                    return numberToEdit.multiply(multiplier)
                }
            } else if (denominator.isOrdinal && denominator.moreThan(2)) {
                return numberToEdit.divide(denominator) // valid denominator, e.g. one fifth
            } else {
                // invalid denominator, e.g. three two, four second
                ts.position = originalPosition // restore to original position
            }
        }
        return numberToEdit
    }

    fun numberSuffixMultiplier(): Number? {
        if (ts[0].hasCategory("suffix_multiplier")) {
            ts.movePositionForwardBy(1)
            return ts[-1].number // a suffix multiplier, e.g. dozen, half, score, percent
        } else if (ts[0].isValue("a") && ts[1].hasCategory("suffix_multiplier")) {
            ts.movePositionForwardBy(2) // also skip "a" before the suffix, e.g. a dozen
            return ts[-1].number // a suffix multiplier preceded by "a", e.g. a quarter
        } else {
            return null
        }
    }

    fun numberSuffixMultiplierInteger(): Number? {
        if (ts[0].hasCategory("suffix_multiplier") && ts[0].number!!.isInteger) {
            ts.movePositionForwardBy(1)
            return ts[-1].number // a suffix multiplier, e.g. dozen, score
        } else if (ts[0].isValue("a") && ts[1].hasCategory("suffix_multiplier")
            && ts[1].number!!.isInteger
        ) {
            ts.movePositionForwardBy(2) // also skip "a" before the suffix, e.g. a dozen
            return ts[-1].number // a suffix multiplier preceded by "a", e.g. a score
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
        var n = numberInteger(allowOrdinal)
        if (n != null && n.isOrdinal) {
            return n // no point or fraction separator can appear after an ordinal number
        }

        if (ts[0].hasCategory("point")) {
            // parse point indicator from e.g. "twenty one point four five three"

            if (!ts[1].hasCategory("digit_after_point")
                && (!NumberExtractorUtils.isRawNumber(ts[1]) || ts[2].hasCategory("ordinal_suffix"))
            ) {
                // also return if next up is an ordinal raw number, i.e. followed by st/nd/rd/th
                return n // there is an only point at the end of the number: it is not part of it
            }

            ts.movePositionForwardBy(1)
            if (n == null) {
                n = Number(0.0) // numbers can start with just "point"
            }

            var magnitude = 0.1
            if (ts[0].value.length > 1 && NumberExtractorUtils.isRawNumber(ts[0])) {
                // handle sequence of raw digits after point, e.g. .0123
                // value.length > 1 since multiple single-digits are handled below, e.g. . 0 1 2 3
                for (i in 0 until ts[0].value.length) {
                    n = n!!.plus((ts[0].value[i].code - '0'.code) * magnitude)
                    magnitude /= 10.0
                }
                ts.movePositionForwardBy(1)
            } else {
                // read as many digits as possible, e.g. point one six 5 one 0 three
                while (true) {
                    if (ts[0].hasCategory("digit_after_point")
                        || (ts[0].value.length == 1 && NumberExtractorUtils.isRawNumber(
                            ts[0]
                        )
                                && !ts[1].hasCategory("ordinal_suffix"))
                    ) {
                        // do not allow ordinal raw numbers, i.e. followed by st/nd/rd/th

                        n = n!!.plus(ts[0].number!!.multiply(magnitude))
                        magnitude /= 10.0
                    } else {
                        break // reached a word that is not a valid digit
                    }
                    ts.movePositionForwardBy(1)
                }
            }
        } else if (n != null && ts[0].hasCategory("fraction_separator")) {
            // parse fraction from e.g. "twenty divided by one hundred"

            val originalPosition = ts.position
            ts.movePositionForwardBy(1)
            if (ts[0].hasCategory("fraction_separator_secondary")) {
                ts.movePositionForwardBy(1) // also remove "by" after "divided by"
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
        val originalPosition = ts.position
        // this avoids matching "and seven", "a two", "a hundredth" and "a trillionth",
        // but still allows "a hundred" and "a trillion"
        if (ts[0].isValue("a") && (
                    (ts[1].hasCategory("multiplier") && !ts[1].number!!.isOrdinal)
                            || ts[1].hasCategory("hundred")
                    )
        ) {
            // allow ignoring "a" if it comes right before the multiplier (e.g. a thousand)
            ts.movePositionForwardBy(1)
        }

        var n = NumberExtractorUtils.numberMadeOfGroups(ts) { ts, lastMultiplier ->
            if (shortScale)
                NumberExtractorUtils.numberGroupShortScale(ts, allowOrdinal, lastMultiplier)
            else
                numberGroupLongScale(ts, allowOrdinal, lastMultiplier)
        }
        if (n == null) {
            // restore original position, "a" can't come before a random raw number, e.g. a 1207
            ts.position = originalPosition
            // try to parse big raw numbers (>=1000), e.g. 1207
            return NumberExtractorUtils.numberBigRaw(ts, allowOrdinal)
        } else if (n.isOrdinal) {
            return n // no more checks, as the ordinal word comes last, e.g. million twelfth
        }

        // n != null from here on
        if (n.lessThan(21) && n.moreThan(9) && !ts[-1].hasCategory("raw")) {
            // parse years (1001 to 2099) in the particular forms (but xx-hundred is handled below)
            val secondGroup = numberYearSecondGroup(allowOrdinal)
            if (secondGroup != null) {
                return n.multiply(100).plus(secondGroup).withOrdinal(secondGroup.isOrdinal)
            }
        }

        if (n.lessThan(100)) {
            val nextNotIgnore = ts.indexOfWithoutCategory("ignore", 0)
            if (ts[nextNotIgnore].hasCategory("hundred")) {
                // parse numbers suffixed by hundred, e.g. twenty six hundred -> 2600
                val ordinal = ts[nextNotIgnore].hasCategory("ordinal")
                if (allowOrdinal || !ordinal) {
                    // prevent ordinal numbers if allowOrdinal is false
                    ts.movePositionForwardBy(nextNotIgnore + 1)
                    return n.multiply(100).withOrdinal(ordinal)
                }
            }
        }

        if (n.lessThan(1000)) {
            // parse raw number n separated by comma, e.g. 123,045,006
            // assuming current position is at the first comma
            if (NumberExtractorUtils.isRawNumber(ts[-1]) && ts[0].hasCategory("thousand_separator") &&
                ts[1].value.length == 3 && NumberExtractorUtils.isRawNumber(ts[1])
            ) {
                val originalPosition = ts.position - 1

                while (ts[0].hasCategory("thousand_separator") && ts[1].value.length == 3 &&
                    NumberExtractorUtils.isRawNumber(ts[1])
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

    fun numberYearSecondGroup(allowOrdinal: Boolean): Number? {
        // parse the last two digits of a year, e.g. oh five -> 05, nineteen -> 19, eighty two -> 82

        // use nextNotIgnore to skip -, e.g. (nineteen)-oh-two

        val nextNotIgnore = ts.indexOfWithoutCategory("ignore", 0)

        if (ts[nextNotIgnore].isNumberEqualTo(0)) {
            val digitIndex = ts.indexOfWithoutCategory("ignore", nextNotIgnore + 1)
            val ordinal = ts[digitIndex].hasCategory("ordinal")
            if (ts[digitIndex].number?.lessThan(10) == true && (allowOrdinal || !ordinal)) {
                // o/oh/nought/zero/0 + digit, e.g. (sixteen) oh one -> (16)01
                // prevent ordinal number if allowOrdinal is false, e.g. (eighteen) oh second
                ts.movePositionForwardBy(digitIndex + 1)
                return ts[-1].number!!.withOrdinal(ordinal)
            }
        } else if (ts[nextNotIgnore].hasCategory("teen")) {
            // teen, e.g. (twenty) thirteen -> (20)13
            val ordinal = ts[nextNotIgnore].hasCategory("ordinal")
            if (!allowOrdinal && ordinal) {
                return null // do not allow ordinal number if allowOrdinal is false
            } else {
                ts.movePositionForwardBy(nextNotIgnore + 1)
                return ts[-1].number!!.withOrdinal(ordinal)
            }
        } else if (ts[nextNotIgnore].value.length == 2
            && NumberExtractorUtils.isRawNumber(ts[nextNotIgnore])
        ) {
            // raw number with two digits, e.g. (twenty) 41 -> (20)41, (12) 05 th -> (12)05th
            val ordinal = ts[nextNotIgnore + 1].hasCategory("ordinal_suffix")
            if (!allowOrdinal && ordinal) {
                return null // do not allow raw number + st/nd/rd/th if allowOrdinal is false
            } else {
                ts.movePositionForwardBy(nextNotIgnore + (if (ordinal) 2 else 1))
                return ts[if (ordinal) -2 else -1].number!!.withOrdinal(ordinal)
            }
        } else if (ts[nextNotIgnore].hasCategory("tens")) {
            // tens (+ digit), e.g. (nineteen) eighty four -> (19)84
            val tens = ts[nextNotIgnore].number
            if (ts[nextNotIgnore].hasCategory("ordinal")) {
                if (allowOrdinal) {
                    // nothing follows an ordinal number, e.g. (twenty) twentieth -> 2020th
                    ts.movePositionForwardBy(nextNotIgnore + 1)
                    return tens!!.withOrdinal(true)
                } else {
                    return null // prevent ordinal numbers if allowOrdinal is false
                }
            }
            ts.movePositionForwardBy(nextNotIgnore + 1)

            val digitIndex = ts.indexOfWithoutCategory("ignore", 0)
            val ordinal = ts[digitIndex].hasCategory("ordinal")
            if (ts[digitIndex].hasCategory("digit") && (allowOrdinal || !ordinal)) {
                // do not consider ordinal digit if allowOrdinal is false
                ts.movePositionForwardBy(digitIndex + 1)
                return tens!!.plus(ts[-1].number).withOrdinal(ordinal)
            } else {
                return tens // digit is optional, e.g. (seventeen) fifty -> (17)50
            }
        }

        return null // invalid second year group
    }

    companion object {
        @JvmStatic
        fun numberGroupLongScale(
            ts: TokenStream,
            allowOrdinal: Boolean,
            lastMultiplier: Double
        ): Number? {
            if (lastMultiplier < 1000000) {
                return null // prevent two numbers smaller than 1000000 to be one after another
            }

            val originalPosition = ts.position
            var first = NumberExtractorUtils.numberGroupShortScale(ts, allowOrdinal, 1000000.0)
            if (first == null) {
                // there is no number or the number is followed by a multiplier which is not thousand
                first = NumberExtractorUtils.numberLessThan1000(ts, allowOrdinal)
                if (first != null && first.isOrdinal) {
                    return first
                }

                if (first == null) {
                    if (NumberExtractorUtils.isRawNumber(ts[0])
                        && ts[0].number!!.lessThan(1000000)
                    ) {
                        // maybe a raw number smaller than 1000000, e.g. 785743
                        val ordinal = ts[1].hasCategory("ordinal_suffix")
                        if (ordinal) {
                            if (!allowOrdinal) {
                                // do not allow raw number + st/nd/rd/th if allowOrdinal is false
                                return null
                            }
                            ts.movePositionForwardBy(2)
                            return ts[-2].number!!.withOrdinal(true)
                        }
                        ts.movePositionForwardBy(1)
                        first = ts[-1].number // raw number group, e.g. 123042 million
                    }
                }
            } else {
                if (first.isOrdinal || first.lessThan(1000)) {
                    // nothing else follows an ordinal number; the number does not end with thousand
                    return first
                }

                val nextNotIgnore = ts.indexOfWithoutCategory("ignore", 0)
                ts.movePositionForwardBy(nextNotIgnore)
                val second = NumberExtractorUtils.numberLessThan1000(ts, allowOrdinal)
                if (second == null) {
                    ts.movePositionForwardBy(-nextNotIgnore)
                } else {
                    first = first.plus(second)
                    if (second.isOrdinal) {
                        return first.withOrdinal(true) // nothing else follows an ordinal number
                    }
                }
            }

            val nextNotIgnore = ts.indexOfWithoutCategory("ignore", 0)
            val ordinal = ts[nextNotIgnore].hasCategory("ordinal")
            if (ts[nextNotIgnore].hasCategory("multiplier") && (allowOrdinal || !ordinal)
                && ts[nextNotIgnore].number!!.moreThan(1000)
            ) {
                // prevent ordinal multiplier if allowOrdinal is false; prevent thousand multiplier
                val multiplier = shortMultiplierToLongScale(ts[nextNotIgnore].number)
                if (multiplier!!.lessThan(lastMultiplier)) {
                    ts.movePositionForwardBy(nextNotIgnore + 1)
                    return if (first == null) {
                        // the multiplier alone, e.g. a million
                        multiplier.withOrdinal(ordinal)
                    } else {
                        // number smaller than 1000000 followed by a multiplier,
                        // e.g. thirteen thousand billion
                        multiplier.multiply(first).withOrdinal(ordinal)
                    }
                }
            } else {
                // no multiplier for this last number group, e.g. one thousand, three hundred and two
                // also here if the multiplier is ordinal, but allowOrdinal is false
                // also here if there is a thousand multiplier (happens e.g. in one thousand thousand)
                return first
            }

            // invalid multiplier or missing multiplier with big group value, reset to previous position
            ts.position = originalPosition
            return null
        }

        fun shortMultiplierToLongScale(shortScaleMultiplier: Number?): Number? {
            return if (shortScaleMultiplier!!.integerValue() == 1000000000L) {
                Number(1000000000000L) // billion
            } else if (shortScaleMultiplier.integerValue() == 1000000000000L) {
                Number(1000000000000000000L) // trillion
            } else if (shortScaleMultiplier.integerValue() == 1000000000000000L) {
                Number(1e24) // quadrillion
            } else if (shortScaleMultiplier.integerValue() == 1000000000000000000L) {
                Number(1e30) // quintillion
            } else {
                shortScaleMultiplier // million
            }
        }
    }
}
