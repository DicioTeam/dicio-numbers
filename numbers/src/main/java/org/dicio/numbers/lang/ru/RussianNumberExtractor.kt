package org.dicio.numbers.lang.ru

import org.dicio.numbers.parser.lexer.TokenStream
import org.dicio.numbers.unit.Number
import org.dicio.numbers.util.NumberExtractorUtils

/**
 * Russian numbers are made of separate words like in English (двадцать три), but hundreds are
 * single words (двести, триста), there is no "hundred" multiplier, and every word has many
 * inflected forms, all of which are listed in the tokenizer config.
 */
class RussianNumberExtractor internal constructor(private val ts: TokenStream) {
    fun numberPreferOrdinal(): Number? {
        val number = numberSuffixMultiplier() // first try with suffix multiplier, e.g. дюжина
            ?: numberSignPoint(true) // then try with normal number

        return number?.let(::withHalfOrDenominator)
    }

    fun numberPreferFraction(): Number? {
        val number = numberSuffixMultiplier() // first try with suffix multiplier, e.g. дюжина
            ?: numberSignPoint(false) // then try without ordinal

        return if (number == null) {
            numberSignPoint(true) // maybe an ordinal number?
        } else {
            withHalfOrDenominator(number)
        }
    }

    fun numberNoOrdinal(): Number? {
        // used for duration parsing
        val number = numberSuffixMultiplier() // first try with suffix multiplier, e.g. пара
            ?: numberSignPoint(false) // then try without ordinal

        return number?.let(::withHalfOrDenominator)
    }

    fun numberMustBeInteger(): Number? {
        val number = numberSuffixMultiplierInteger() // first try with suffix multiplier, e.g. пара
            ?: numberSignInteger(true) // then try with normal number
            ?: return null

        val multiplier = numberSuffixMultiplierInteger()
        return if (multiplier == null) number else number.multiply(multiplier)
    }


    /**
     * Handles what can follow a number: "с половиной" (два с половиной = 2.5), an ordinal
     * denominator (две пятых = 2/5) or a suffix multiplier (две дюжины = 24).
     */
    fun withHalfOrDenominator(numberToEdit: Number): Number {
        if (numberToEdit.isOrdinal || numberToEdit.isDecimal) {
            return numberToEdit
        }

        if (ts[0].hasCategory("half_with") && ts[1].hasCategory("half_after_with")) {
            ts.movePositionForwardBy(2)
            return numberToEdit.plus(0.5) // e.g. два с половиной
        }

        if (ts[0].hasCategory("ignore")) {
            return numberToEdit
        }

        val originalPosition = ts.position
        val denominator = numberInteger(true)
        if (denominator == null) {
            // no denominator found: maybe a custom multiplier? e.g. половина (=0.5), дюжина (=12)
            if (ts[0].hasCategory("suffix_multiplier")) {
                ts.movePositionForwardBy(1)
                val multiplier = ts[-1].number!!
                if (multiplier.isDecimal &&
                    (1 / multiplier.decimalValue()).toLong().toDouble() == (1 / multiplier.decimalValue())
                ) {
                    // the multiplier is an exact fraction, divide by the denominator converted to
                    // long to possibly preserve the integerness of numberToEdit, e.g. две четверти
                    return numberToEdit.divide((1 / multiplier.decimalValue()).toLong())
                }
                return numberToEdit.multiply(multiplier)
            }
        } else if (denominator.isOrdinal && denominator.moreThan(2)) {
            return numberToEdit.divide(denominator) // valid denominator, e.g. одна пятая
        } else {
            ts.position = originalPosition // invalid denominator, e.g. шесть первых
        }
        return numberToEdit
    }

    fun numberSuffixMultiplier(): Number? {
        return if (ts[0].hasCategory("suffix_multiplier")) {
            ts.movePositionForwardBy(1)
            ts[-1].number // a suffix multiplier, e.g. половина, пара, дюжина, процент
        } else {
            null
        }
    }

    fun numberSuffixMultiplierInteger(): Number? {
        return if (ts[0].hasCategory("suffix_multiplier") && ts[0].number!!.isInteger) {
            ts.movePositionForwardBy(1)
            ts[-1].number // a suffix multiplier, e.g. пара, дюжина
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
        if (ts[0].hasCategory("one_and_half")) {
            ts.movePositionForwardBy(1)
            return ts[-1].number // полтора, полторы
        }

        var n = numberInteger(allowOrdinal).let {
            if (it == null || it.isOrdinal) {
                // numbers can not start with just "запятая", and no point or fraction separator
                // can appear after an ordinal number
                return@numberPoint it
            }
            it
        }

        if (ts[0].hasCategory("whole_point")) {
            // e.g. "три целых четырнадцать сотых"
            val originalPosition = ts.position
            ts.movePositionForwardBy(1)
            val numerator = numberInteger(false)
            val denominator = numerator?.let { numberInteger(true) }
            if (numerator != null && denominator != null && denominator.isOrdinal
                && denominator.moreThan(1)
            ) {
                return n.plus(numerator.divide(denominator).decimalValue())
            }
            ts.position = originalPosition
            return n

        } else if (ts[0].hasCategory("point")) {
            // parse point indicator from e.g. "двадцать один запятая четыре пять три", "2,5"
            if (!ts[1].hasCategory("digit_after_point")
                && (!NumberExtractorUtils.isRawNumber(ts[1]) || ordinalSuffixLength(1) > 0)
            ) {
                return n // there is an only comma at the end of the number: it is not part of it
            }
            ts.movePositionForwardBy(1)

            var magnitude = 0.1
            if (ts[0].value.length > 1 && NumberExtractorUtils.isRawNumber(ts[0])) {
                // handle sequence of raw digits after point, e.g. ,0123
                for (i in 0 until ts[0].value.length) {
                    n = n.plus((ts[0].value[i].code - '0'.code) * magnitude)
                    magnitude /= 10.0
                }
                ts.movePositionForwardBy(1)
            } else {
                // read as many digits as possible, e.g. запятая один шесть 5 один 0 три
                while (ts[0].hasCategory("digit_after_point")
                    || (ts[0].value.length == 1 && NumberExtractorUtils.isRawNumber(ts[0])
                            && ordinalSuffixLength(0) == 0)
                ) {
                    n = n.plus(ts[0].number!!.multiply(magnitude))
                    magnitude /= 10.0
                    ts.movePositionForwardBy(1)
                }
            }

        } else if (ts[0].hasCategory("fraction_separator")) {
            // parse fraction from e.g. "двадцать делить на сто"
            var separatorLength = 1
            if (ts[1].hasCategory("fraction_separator_secondary")) {
                separatorLength = 2 // also remove "на" after "делить"
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
        } ?: return numberBigRaw(allowOrdinal) // e.g. 1207

        if (n.isOrdinal || !n.lessThan(1000) || !NumberExtractorUtils.isRawNumber(ts[-1])) {
            return n
        }

        // raw number with groups of three digits separated by spaces, e.g. 1 000 000
        val originalPosition = ts.position
        while (ts[-1].spacesFollowing.isNotEmpty() && ts[0].value.length == 3
            && NumberExtractorUtils.isRawNumber(ts[0]) && ts[0].number!!.isInteger
        ) {
            n = n.multiply(1000).plus(ts[0].number)
            ts.movePositionForwardBy(1)
        }
        if (ts.position == originalPosition) {
            return n
        }

        val suffix = ordinalSuffixLength(-1)
        if (suffix > 0) {
            if (!allowOrdinal) {
                ts.position = originalPosition
                return n
            }
            ts.movePositionForwardBy(suffix)
            return n.withOrdinal(true) // e.g. 1 000-й
        }
        return n
    }

    private fun numberBigRaw(allowOrdinal: Boolean): Number? {
        if (!NumberExtractorUtils.isRawNumber(ts[0])) {
            return null
        }
        val suffix = ordinalSuffixLength(0)
        if (suffix > 0 && !allowOrdinal) {
            return null
        }
        ts.movePositionForwardBy(1 + suffix)
        return ts[-1 - suffix].number!!.withOrdinal(suffix > 0) // e.g. 1250067, 2027-го
    }

    private fun numberGroup(allowOrdinal: Boolean, lastMultiplier: Double): Number? {
        if (lastMultiplier < 1000) {
            return null // prevent two numbers smaller than 1000 to be one after another
        }

        val originalPosition = ts.position
        val groupValue = numberLessThan1000(allowOrdinal) // e.g. сто двенадцать
        if (groupValue != null && groupValue.isOrdinal) {
            return groupValue // ordinal numbers can't be followed by a multiplier
        }

        val nextNotIgnore = if (groupValue == null) 0 else ts.indexOfWithoutCategory("ignore", 0)
        val ordinal = ts[nextNotIgnore].hasCategory("ordinal")
        if (ts[nextNotIgnore].hasCategory("multiplier") && (allowOrdinal || !ordinal)) {
            val multiplier = ts[nextNotIgnore].number!!
            if (multiplier.lessThan(lastMultiplier)) {
                ts.movePositionForwardBy(nextNotIgnore + 1)
                return if (groupValue == null) {
                    multiplier.withOrdinal(ordinal) // the multiplier alone, e.g. тысяча
                } else {
                    multiplier.multiply(groupValue).withOrdinal(ordinal) // e.g. три миллиона
                }
            }
        } else {
            return groupValue // no multiplier for this last number group, e.g. сто два
        }

        // multiplier is too big, reset to previous position
        ts.position = originalPosition
        return null
    }

    private fun numberLessThan1000(allowOrdinal: Boolean): Number? {
        var hundred: Long = -1
        var ten: Long = -1
        var digit: Long = -1
        var ordinal = false
        var firstIteration = true
        while (true) {
            val nextNotIgnore = if (firstIteration) {
                firstIteration = false
                0 // do not skip ahead if nothing was matched so far
            } else {
                ts.indexOfWithoutCategory("ignore", 0)
            }
            val token = ts[nextNotIgnore]

            if (!allowOrdinal && token.hasCategory("ordinal")) {
                break // prevent ordinal numbers if allowOrdinal is false
            }

            if (token.hasCategory("hundreds")) {
                if (hundred < 0 && ten < 0 && digit < 0) {
                    hundred = token.number!!.integerValue() // e.g. двести
                } else {
                    break // unexpected double hundred or hundred after ten/digit
                }
            } else if (token.hasCategory("teen")) {
                if (ten < 0 && digit < 0) {
                    ten = token.number!!.integerValue()
                    digit = 0 // ten contains also the digit, set to 0 to prevent double digit
                } else {
                    break
                }
            } else if (token.hasCategory("tens")) {
                if (ten < 0 && digit < 0) {
                    ten = token.number!!.integerValue()
                } else {
                    break
                }
            } else if (token.hasCategory("digit")) {
                if (digit < 0 && (!token.isNumberEqualTo(0) || (ten < 0 && hundred < 0))) {
                    // do not allow zero after ten or hundred, e.g. двадцать ноль
                    digit = token.number!!.integerValue()
                } else {
                    break // unexpected double digit
                }
            } else if (NumberExtractorUtils.isRawNumber(token)) {
                val rawNumber = token.number!!
                if (rawNumber.isDecimal || hundred >= 0 || ten >= 0 || digit >= 0) {
                    break // raw numbers are not mixed with words, e.g. сто 5
                }
                val suffix = ordinalSuffixLength(nextNotIgnore)
                if (!allowOrdinal && suffix > 0) {
                    break
                }
                if (!rawNumber.lessThan(1000)) {
                    break // raw number is too big, handled by numberBigRaw
                }
                hundred = rawNumber.integerValue()
                ten = 0
                digit = 0
                if (suffix > 0) {
                    ordinal = true
                    ts.movePositionForwardBy(nextNotIgnore + 1 + suffix)
                    break // raw number followed by an ordinal suffix, e.g. 5-го
                }
            } else {
                break // random token encountered, number is terminated
            }

            ts.movePositionForwardBy(nextNotIgnore + 1)
            if (ts[-1].hasCategory("ordinal")) {
                ordinal = true
                break // ordinal number encountered, nothing else can follow, e.g. сто пятый
            }
        }

        return if (hundred < 0 && ten < 0 && digit < 0) {
            null
        } else {
            Number(
                (if (hundred < 0) 0 else hundred) + (if (ten < 0) 0 else ten)
                        + (if (digit < 0) 0 else digit), ordinal
            )
        }
    }

    /**
     * @param rawIndex the position (relative to the current one) of a raw number
     * @return how many tokens make up the ordinal suffix attached to the raw number, e.g. 2 for
     * "5-го" ("-" and "го"), 1 for "5го", 0 if there is no ordinal suffix
     */
    private fun ordinalSuffixLength(rawIndex: Int): Int {
        if (ts[rawIndex].spacesFollowing.isNotEmpty()) {
            return 0
        }
        var suffixIndex = rawIndex + 1
        if (ts[suffixIndex].value == "-") {
            if (ts[suffixIndex].spacesFollowing.isNotEmpty()) {
                return 0
            }
            suffixIndex += 1
        }
        val suffix = ts[suffixIndex]
        // "й" is normalized to "и", which is the "ignore" conjunction, so it is checked by value
        return if (suffix.hasCategory("ordinal_suffix") || suffix.value.lowercase() == "й") {
            suffixIndex - rawIndex
        } else {
            0
        }
    }
}
