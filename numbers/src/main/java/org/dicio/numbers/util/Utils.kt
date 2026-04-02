package org.dicio.numbers.util

import java.util.regex.Pattern
import kotlin.math.abs

object Utils {
    const val WHOLE_NUMBER_ACCURACY: Double = 0.0001
    const val WHOLE_FRACTION_ACCURACY: Double = 0.01

    private val DUPLICATE_SPACES_PATTERN: Pattern = Pattern.compile("  +")

    /**
     * @param number the number to check
     * @param accuracy the maximum distance to consider valid between the number and its nearest
     * whole number
     * @return whether the provided number is a whole number or a really near one (i.e. within the
     * provided accuracy).
     */
    @JvmStatic
    fun isWhole(number: Double, accuracy: Double): Boolean {
        return abs(number - Math.round(number)) < accuracy
    }

    /**
     * Counts the useful decimal places that need to be considered. Formatting the number with the
     * returned count will not yield any zeros at the end.
     * @param number the number to count places of
     * @param maxPlaces the maximum number of places to return
     * @return the count of useful decimal places to be considered
     */
    @JvmStatic
    fun decimalPlacesNoFinalZeros(number: Double, maxPlaces: Int): Int {
        val formatted = String.format("%." + maxPlaces + "f", abs(number) % 1)

        var realPlaces = maxPlaces
        while (realPlaces > 0 && formatted[realPlaces + 1] == '0') {
            --realPlaces
        }

        return realPlaces
    }

    /**
     * @param n the base
     * @param exponent the exponent
     * @return n ^ exponent
     */
    @JvmStatic
    fun longPow(n: Long, exponent: Int): Long {
        var result: Long = 1
        for (e in 0 until exponent) {
            result *= n
        }
        return result
    }

    /**
     * @param number the number to round
     * @return the nearest long to the number
     */
    @JvmStatic
    fun roundToLong(number: Double): Long {
        return if (number < 0) {
            number.toLong() + (if (number % 1 <= -0.5) -1 else 0)
        } else {
            number.toLong() + (if (number % 1 >= 0.5) 1 else 0)
        }
    }

    /**
     * @param number the number on which to operate
     * @return the difference between the number and the number rounded to long
     */
    fun remainderFromRoundingToLong(number: Double): Double {
        return number - roundToLong(number)
    }

    /**
     * @param number the number to round
     * @return the nearest int to the number
     */
    fun roundToInt(number: Double): Int {
        return if (number < 0) {
            number.toInt() + (if (number % 1 <= -0.5) -1 else 0)
        } else {
            number.toInt() + (if (number % 1 >= 0.5) 1 else 0)
        }
    }

    /**
     * @param n the number to split
     * @param splitModulus the modulus to apply to split the number
     * @return the splits (which could be 0), in opposite order,
     * e.g. `splitByModulus(10294000, 1000) -> [0, 294, 10]`
     */
    @JvmStatic
    fun splitByModulus(n: Long, splitModulus: Int): List<Long> {
        var nVar = n
        val result: MutableList<Long> = ArrayList()
        while (nVar > 0) {
            result.add(nVar % splitModulus)
            nVar /= splitModulus.toLong()
        }
        return result
    }

    /**
     * @param s the string to clean
     * @return the original string but without leading, trailing or duplicate spaces
     */
    @JvmStatic
    fun removeRedundantSpaces(s: String): String {
        return DUPLICATE_SPACES_PATTERN.matcher(s).replaceAll(" ").trim { it <= ' ' }
    }

    /**
     * @param s the string loop through and check
     * @param codePoint the code point to find
     * @return whether the code point is present inside the string or not
     */
    @JvmStatic
    fun containsCodePoint(s: String, codePoint: Int): Boolean {
        var i = 0
        while (i < s.length) {
            val cp = Character.codePointAt(s, i)
            if (cp == codePoint) return true
            i += Character.charCount(cp)
        }
        return false
    }

    /**
     * Calls the provided suppliers in order until one returns a non-null value, in which case
     * returns such value. If all suppliers return null, this function in turn returns null.
     *
     * @param suppliers the suppliers to try to call, in order
     * @param <T> the return type of the suppliers and of this function
     * @return the result of the first supplier with a non-null value, or null
    </T> */
    @JvmStatic
    @SafeVarargs
    fun <T> firstNotNull(vararg suppliers: () -> T): T? {
        for (supplier in suppliers) {
            val result: T? = supplier()
            if (result != null) {
                return result
            }
        }
        return null
    }
}
