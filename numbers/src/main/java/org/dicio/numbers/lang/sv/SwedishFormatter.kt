package org.dicio.numbers.lang.sv

import org.dicio.numbers.formatter.Formatter
import org.dicio.numbers.unit.MixedFraction
import org.dicio.numbers.util.Utils
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

class SwedishFormatter : Formatter("config/sv-se") {

    override fun niceNumber(mixedFraction: MixedFraction, speech: Boolean): String {
        if (speech) {
            val sign = if (mixedFraction.negative) "minus " else ""
            if (mixedFraction.numerator == 0) {
                return sign + pronounceNumber(mixedFraction.whole.toDouble(), 0, true, false, false)
            }

            var denominatorString = when (mixedFraction.denominator) {
                2 -> "halv"
                4 -> "kvarts"
                else -> {
                    // use ordinal: only halv and kvarts are exceptions
                    pronounceNumber(mixedFraction.denominator.toDouble(), 0, true, false, true)
                }
            }

            val numeratorString: String
            if (mixedFraction.numerator == 1) {
                numeratorString = "en"
            } else {
                numeratorString =
                    pronounceNumber(mixedFraction.numerator.toDouble(), 0, true, false, false)
                denominatorString += "ar"
            }

            return if (mixedFraction.whole == 0L) {
                "$sign$numeratorString $denominatorString"
            } else {
                (sign + pronounceNumber(mixedFraction.whole.toDouble(), 0, true, false, false)
                        + " och " + numeratorString + " " + denominatorString)
            }
        } else {
            return niceNumberNotSpeech(mixedFraction)
        }
    }

    override fun pronounceNumber(
        number: Double,
        places: Int,
        shortScale: Boolean,
        scientific: Boolean,
        ordinal: Boolean
    ): String {
        if (number == Double.POSITIVE_INFINITY) {
            return "oändlighet"
        } else if (number == Double.NEGATIVE_INFINITY) {
            return "negativ oändlighet"
        } else if (java.lang.Double.isNaN(number)) {
            return "inte ett tal"
        }

        // also using scientific mode if the number is too big to be spoken fully
        if (scientific || abs(number) > 999999999999999934463.0) {
            val scientificFormatted = String.format(Locale.ENGLISH, "%E", number)
            val parts = scientificFormatted.split("E".toRegex(), limit = 2).toTypedArray()
            val power = parts[1].toInt().toDouble()

            if (power != 0.0) {
                val n = parts[0].toDouble()
                return String.format(
                    "%s%s gånger tio upphöjt till %s%s",
                    if (n < 0) "negativ " else "",
                    pronounceNumber(abs(n), places, true, false, false),
                    if (power < 0) "negativ " else "",
                    pronounceNumber(abs(power), places, true, false, false)
                )
            }
        }

        val result = StringBuilder()
        var varNumber = number
        if (varNumber < 0) {
            varNumber = -varNumber
            if (places != 0 || varNumber >= 0.5) {
                result.append(if (scientific) "negativ " else "minus ")
            }
        }

        val realPlaces = Utils.decimalPlacesNoFinalZeros(varNumber, places)
        val numberIsWhole = realPlaces == 0
        val numberLong = varNumber.toLong() + (if (varNumber % 1 >= 0.5 && numberIsWhole) 1 else 0)

        // Sweden always uses long scale (miljon, miljard, biljon, ...)
        result.append(cardinalOrOrdinal(numberLong, ordinal && numberIsWhole))

        if (realPlaces > 0) {
            if (varNumber < 1.0 && (result.isEmpty() || "minus ".contentEquals(result))) {
                result.append("noll")
            }
            result.append(" komma")

            val fractionalPart = String.format("%." + realPlaces + "f", varNumber % 1)
            for (i in 2 until fractionalPart.length) {
                result.append(" ")
                result.append(NUMBER_NAMES[(fractionalPart[i].code - '0'.code).toLong()])
            }
        }

        return result.toString()
    }

    override fun niceTime(
        time: LocalTime,
        speech: Boolean,
        use24Hour: Boolean,
        showAmPm: Boolean
    ): String {
        if (speech) {
            if (use24Hour) {
                val result = StringBuilder()
                result.append("klockan ")
                result.append(pronounceNumberNeuter(time.hour.toLong()))

                if (time.minute == 0) {
                    // "klockan tretton" (nothing more)
                } else {
                    result.append(" ")
                    if (time.hour == 0) {
                        // the hour is spoken as two digits, i.e. 00:05 is "noll noll noll fem"
                        result.append("noll ")
                    }
                    if (time.minute < 10) {
                        result.append("noll ")
                    }
                    result.append(pronounceNumberNeuter(time.minute.toLong()))
                }

                return result.toString()
            } else {
                if (time.hour == 0 && time.minute == 0) {
                    return "midnatt"
                }

                val normalizedHour = (time.hour + 11) % 12 + 1
                val result = StringBuilder()
                if (time.minute == 15) {
                    result.append("kvart över ")
                    result.append(pronounceNumberNeuter(normalizedHour.toLong()))
                } else if (time.minute == 30) {
                    result.append("halv ")
                    result.append(pronounceNumberNeuter((normalizedHour % 12 + 1).toLong()))
                } else if (time.minute == 45) {
                    result.append("kvart i ")
                    result.append(pronounceNumberNeuter((normalizedHour % 12 + 1).toLong()))
                } else {
                    result.append(pronounceNumberNeuter(normalizedHour.toLong()))

                    if (time.minute == 0) {
                        // nothing more to add
                    } else {
                        result.append(" ")
                        if (time.minute < 10) {
                            result.append("noll ")
                        }
                        result.append(pronounceNumberNeuter(time.minute.toLong()))
                    }
                }

                if (showAmPm) {
                    result.append(" ").append(partOfDay(time.hour))
                }
                return result.toString()
            }
        } else {
            if (use24Hour) {
                return time.format(DateTimeFormatter.ofPattern("HH:mm", Locale("sv", "SE")))
            } else {
                val result = time.format(
                    DateTimeFormatter.ofPattern(
                        if (showAmPm) "K:mm a" else "K:mm", Locale("sv", "SE")
                    )
                )
                return if (result.startsWith("0:")) {
                    "12:" + result.substring(2)
                } else {
                    result
                }
            }
        }
    }


    /**
     * Swedish has no am/pm, so the part of the day is spoken instead. Boundaries:
     * 00:00-04:59 "på natten", 05:00-09:59 "på morgonen", 10:00-11:59 "på förmiddagen",
     * 12:00-17:59 "på eftermiddagen", 18:00-23:59 "på kvällen".
     */
    private fun partOfDay(hour: Int): String {
        return when (hour) {
            in 0..4 -> "på natten"
            in 5..9 -> "på morgonen"
            in 10..11 -> "på förmiddagen"
            in 12..17 -> "på eftermiddagen"
            else -> "på kvällen"
        }
    }

    /** Used for durations: all units of time (dag, timme, minut, sekund) are common gender. */
    override fun pronounceNumberDuration(number: Long): String {
        return pronounceNumber(number.toDouble(), 0, true, false, false)
            .let { if (it.endsWith("ett")) it.removeSuffix("ett") + "en" else it }
    }

    /** Used for clock hours and minutes ("klockan ett", "tjugoett"). */
    private fun pronounceNumberNeuter(number: Long): String {
        return pronounceNumber(number.toDouble(), 0, true, false, false)
    }

    /**
     * Spells out a whole number. Digits within a group are written together ("tvåhundratjugotre"),
     * groups are separated by spaces ("ettusen etthundra"). The number 100 and 1000 on their own
     * are "hundra" and "tusen", while 1 million, 1 milliard etc. are "en miljon", "en miljard".
     */
    private fun cardinalOrOrdinal(n: Long, ordinal: Boolean): String {
        if (n == 0L) {
            return if (ordinal) "nollte" else "noll"
        }

        val groups = ArrayList<Long>() // lowest group first
        var rest = n
        while (rest > 0) {
            groups.add(rest % 1000)
            rest /= 1000
        }

        val parts = ArrayList<String>()
        val lastNonZero = groups.indexOfFirst { it != 0L }
        for (i in groups.indices.reversed()) {
            val g = groups[i]
            if (g == 0L) {
                continue
            }
            val ord = ordinal && i == lastNonZero
            val isTop = i == groups.size - 1
            parts.add(
                when (i) {
                    0 -> subThousand(g, ord, isTop)
                    1 -> {
                        // a lone "tusen" is not "ettusen", but "ettusen etthundra" is
                        val count = when {
                            n == 1000L -> ""
                            g == 1L -> "et"
                            else -> subThousand(g, false, false)
                        }
                        count + (if (ord) "tusende" else "tusen")
                    }
                    else -> {
                        // common gender: "en miljon", "tjugoen miljoner"
                        val count = subThousand(g, false, false).removeSuffix("ett").let {
                            if (it.length != subThousand(g, false, false).length) it + "en" else it
                        }
                        val scale = SCALE_NAMES[i]!!
                        if (ord) {
                            (if (g == 1L && isTop) "" else "$count ") + scale + "te"
                        } else if (g == 1L) {
                            "$count $scale"
                        } else {
                            count + " " + scale + "er"
                        }
                    }
                }
            )
        }
        return parts.joinToString(" ")
    }

    /**
     * @param n must be 0 < n <= 999
     * @param ordinal whether to return an ordinal number
     * @param bareHundred whether 100 should be "hundra" instead of "etthundra"
     * @return the string representation of a number smaller than 1000
     */
    private fun subThousand(n: Long, ordinal: Boolean, bareHundred: Boolean): String {
        if (ordinal && ORDINAL_NAMES.containsKey(n)) {
            return ORDINAL_NAMES[n]!!
        } else if (n < 100) {
            if (!ordinal && NUMBER_NAMES.containsKey(n)) {
                return NUMBER_NAMES[n]!!
            }

            // n is surely => 20 from here on, since all n < 20 are in the maps
            return (NUMBER_NAMES[n - n % 10]
                    + (if (n % 10 > 0) subThousand(n % 10, ordinal, false) else ""))
        } else {
            val hundredPart = if (n / 100 == 1L) {
                if (bareHundred) "hundra" else "etthundra"
            } else {
                NUMBER_NAMES[n / 100] + "hundra"
            }
            return (hundredPart
                    + (if (n % 100 > 0) subThousand(n % 100, ordinal, false)
            else (if (ordinal) "de" else "")))
        }
    }

    companion object {
        val NUMBER_NAMES = mapOf(
            0L to "noll",
            1L to "ett",
            2L to "två",
            3L to "tre",
            4L to "fyra",
            5L to "fem",
            6L to "sex",
            7L to "sju",
            8L to "åtta",
            9L to "nio",
            10L to "tio",
            11L to "elva",
            12L to "tolv",
            13L to "tretton",
            14L to "fjorton",
            15L to "femton",
            16L to "sexton",
            17L to "sjutton",
            18L to "arton",
            19L to "nitton",
            20L to "tjugo",
            30L to "trettio",
            40L to "fyrtio",
            50L to "femtio",
            60L to "sextio",
            70L to "sjuttio",
            80L to "åttio",
            90L to "nittio",
            100L to "hundra",
            1000L to "tusen",
            1000000L to "miljon",
        )

        private val SCALE_NAMES = mapOf(
            2 to "miljon",
            3 to "miljard",
            4 to "biljon",
            5 to "biljard",
            6 to "triljon",
        )

        // Sverige använder long scale
        val NUMBER_NAMES_LONG_SCALE = NUMBER_NAMES + mapOf(
            1000000000000L to "biljon",
            1000000000000000000L to "triljon",
        )

        val ORDINAL_NAMES = mapOf(
            1L to "första",
            2L to "andra",
            3L to "tredje",
            4L to "fjärde",
            5L to "femte",
            6L to "sjätte",
            7L to "sjunde",
            8L to "åttonde",
            9L to "nionde",
            10L to "tionde",
            11L to "elfte",
            12L to "tolfte",
            13L to "trettonde",
            14L to "fjortonde",
            15L to "femtonde",
            16L to "sextonde",
            17L to "sjuttonde",
            18L to "artonde",
            19L to "nittonde",
            20L to "tjugonde",
            30L to "trettionde",
            40L to "fyrtionde",
            50L to "femtionde",
            60L to "sextionde",
            70L to "sjuttionde",
            80L to "åttionde",
            90L to "nittionde",
            100L to "hundrade",
            1000L to "tusende",
            1000000L to "miljonte",
        )

        val ORDINAL_NAMES_LONG_SCALE = ORDINAL_NAMES + mapOf(
            1000000000000L to "biljonte",
            1000000000000000000L to "triljonte",
        )
    }
}
