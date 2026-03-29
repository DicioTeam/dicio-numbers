package org.dicio.numbers.lang.fr

import org.dicio.numbers.formatter.Formatter
import org.dicio.numbers.unit.MixedFraction
import org.dicio.numbers.util.Utils
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

class FrenchFormatter : Formatter("config/fr-fr") {

    override fun niceNumber(mixedFraction: MixedFraction, speech: Boolean): String {
        if (speech) {
            val sign = if (mixedFraction.negative) "moins " else ""
            if (mixedFraction.numerator == 0) {
                return sign + pronounceNumber(mixedFraction.whole.toDouble(), 0, true, false, false)
            }

            val denominatorString = when (mixedFraction.denominator) {
                2 -> if (mixedFraction.numerator == 1) "demi" else "demis"
                4 -> if (mixedFraction.numerator == 1) "quart" else "quarts"
                else -> {
                    val base = pronounceNumber(
                        mixedFraction.denominator.toDouble(), 0, true, false, true
                    )
                    if (mixedFraction.numerator == 1) base else base + "s"
                }
            }

            val numeratorString = pronounceNumber(
                mixedFraction.numerator.toDouble(), 0, true, false, false
            )

            return if (mixedFraction.whole == 0L) {
                "$sign$numeratorString $denominatorString"
            } else {
                (sign + pronounceNumber(mixedFraction.whole.toDouble(), 0, true, false, false)
                        + " et " + numeratorString + " " + denominatorString)
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
            return "infini"
        } else if (number == Double.NEGATIVE_INFINITY) {
            return "moins infini"
        } else if (java.lang.Double.isNaN(number)) {
            return "pas un nombre"
        }

        if (scientific || abs(number) > 999999999999999934463.0) {
            val scientificFormatted = String.format(Locale.ENGLISH, "%E", number)
            val parts = scientificFormatted.split("E".toRegex(), limit = 2).toTypedArray()
            val power = parts[1].toInt().toDouble()

            if (power != 0.0) {
                val n = parts[0].toDouble()
                return String.format(
                    "%s fois dix à la %s",
                    pronounceNumber(n, places, shortScale, false, false),
                    pronounceNumber(power, places, shortScale, false, false)
                )
            }
        }

        val result = StringBuilder()
        var varNumber = number
        if (varNumber < 0) {
            varNumber = -varNumber
            if (places != 0 || varNumber >= 0.5) {
                result.append("moins ")
            }
        }

        val realPlaces = Utils.decimalPlacesNoFinalZeros(varNumber, places)
        val numberIsWhole = realPlaces == 0
        val realOrdinal = ordinal && numberIsWhole
        val numberLong = varNumber.toLong() + (if (varNumber % 1 >= 0.5 && numberIsWhole) 1 else 0)

        if (realOrdinal && ORDINAL_NAMES.containsKey(numberLong)) {
            result.append(ORDINAL_NAMES[numberLong])
        } else if (!realOrdinal && NUMBER_NAMES.containsKey(numberLong)) {
            result.append(NUMBER_NAMES[numberLong])
        } else {
            val groups = Utils.splitByModulus(numberLong, 1000)
            val groupNames: MutableList<String> = ArrayList()

            for (i in groups.indices) {
                val z = groups[i]
                if (z == 0L) continue

                var groupName = subThousand(z)

                when {
                    i == 1 -> {
                        // thousands: "mille" is invariable in French
                        groupName = if (z == 1L) "mille" else "$groupName mille"
                    }
                    i != 0 -> {
                        val magnitude = Utils.longPow(1000, i)
                        val magnitudeName = NUMBER_NAMES[magnitude]!!
                        // million/milliard take plural -s when > 1 and not followed by another number
                        val suffix = if (z > 1 && groups.take(i).all { it == 0L }) "s" else ""
                        groupName = if (z == 1L) "un $magnitudeName$suffix"
                        else "$groupName $magnitudeName$suffix"
                    }
                }

                groupNames.add(groupName)
            }

            if (groupNames.isEmpty()) {
                result.append("zéro")
            } else {
                // Groups are in ascending order of magnitude, reverse for output
                for (i in groupNames.indices.reversed()) {
                    if (result.isNotEmpty()) result.append(" ")
                    result.append(groupNames[i])
                }
            }

            if (ordinal && numberIsWhole) {
                return buildOrdinal(numberLong, result.toString())
            }
        }

        if (realPlaces > 0) {
            if (varNumber < 1.0 && (result.isEmpty() || "moins ".contentEquals(result))) {
                result.append("zéro")
            }
            result.append(" virgule")

            val fractionalPart = String.format("%." + realPlaces + "f", varNumber % 1)
            for (i in 2 until fractionalPart.length) {
                result.append(" ")
                result.append(NUMBER_NAMES[(fractionalPart[i].code - '0'.code).toLong()])
            }
        }

        return result.toString().trim()
    }

    override fun niceTime(
        time: LocalTime,
        speech: Boolean,
        use24Hour: Boolean,
        showAmPm: Boolean
    ): String {
        if (speech) {
            val result = StringBuilder()

            when (time.hour) {
                0 -> result.append("minuit")
                12 -> result.append("midi")
                else -> {
                    val hour = if (use24Hour) time.hour else time.hour % 12
                    result.append(pronounceNumberDuration(hour.toLong()))
                    result.append(if (hour <= 1) " heure" else " heures")
                }
            }

            when (time.minute) {
                0 -> { /* nothing */ }
                15 -> result.append(" et quart")
                30 -> result.append(" et demie")
                45 -> {
                    // e.g. "moins le quart" relative to next hour
                    val nextHour = (time.hour + 1) % 24
                    result.clear()
                    result.append(
                        when (nextHour) {
                            0 -> "minuit"
                            12 -> "midi"
                            else -> {
                                val h = if (use24Hour) nextHour else nextHour % 12
                                pronounceNumberDuration(h.toLong()) +
                                        (if (h <= 1) " heure" else " heures")
                            }
                        }
                    )
                    result.append(" moins le quart")
                }
                else -> {
                    result.append(" ")
                    if (time.minute < 10) result.append("zéro ")
                    result.append(pronounceNumberDuration(time.minute.toLong()))
                }
            }

            if (!use24Hour && showAmPm && time.hour != 0 && time.hour != 12) {
                result.append(
                    when {
                        time.hour >= 21 -> " du soir"
                        time.hour >= 17 -> " de l'après-midi"
                        time.hour >= 12 -> " de l'après-midi"
                        time.hour >= 4 -> " du matin"
                        else -> " du matin"
                    }
                )
            }

            return result.toString()
        } else {
            if (use24Hour) {
                return time.format(DateTimeFormatter.ofPattern("HH:mm", Locale.FRENCH))
            } else {
                val result = time.format(
                    DateTimeFormatter.ofPattern(
                        if (showAmPm) "K:mm a" else "K:mm", Locale.ENGLISH
                    )
                )
                return if (result.startsWith("0:")) "12:" + result.substring(2) else result
            }
        }
    }

    override fun pronounceNumberDuration(number: Long): String {
        if (number == 1L) return "un"
        return super.pronounceNumberDuration(number)
    }

    /**
     * Converts n (0-99) to French words.
     */
    private fun subHundred(n: Long): String = when {
        NUMBER_NAMES.containsKey(n) -> NUMBER_NAMES[n]!!
        n < 70 -> {
            val tens = n / 10
            val unit = n % 10
            when {
                unit == 1L -> NUMBER_NAMES[tens * 10]!! + " et un"
                else -> NUMBER_NAMES[tens * 10]!! + "-" + NUMBER_NAMES[unit]!!
            }
        }
        n < 80 -> {
            // 70-79: soixante + (dix..dix-neuf)
            val addon = n - 60  // 10..19
            if (addon == 11L) "soixante et onze"
            else "soixante-" + NUMBER_NAMES[addon]!!
        }
        n == 80L -> "quatre-vingts"
        n < 90 -> "quatre-vingt-" + NUMBER_NAMES[n - 80]!!
        else -> {
            // 90-99: quatre-vingt + (dix..dix-neuf)
            val addon = n - 80  // 10..19
            "quatre-vingt-" + NUMBER_NAMES[addon]!!
        }
    }

    /**
     * Converts n (0-999) to French words.
     */
    private fun subThousand(n: Long): String {
        val builder = StringBuilder()
        if (n >= 100) {
            val hundred = n / 100
            val rest = n % 100
            if (hundred == 1L) {
                builder.append("cent")
            } else {
                builder.append(subHundred(hundred))
                builder.append(" cent")
                if (rest == 0L) builder.append("s") // "deux cents" but "deux cent un"
            }
            if (rest > 0) {
                builder.append(" ")
                builder.append(subHundred(rest))
            }
        } else {
            builder.append(subHundred(n))
        }
        return builder.toString()
    }

    /**
     * Converts a cardinal number string to ordinal by appending "-ième".
     * Handles French-specific rules: strip trailing "s" (quatre-vingts, deux cents),
     * add "u" after "cinq", change "f" to "v" for "neuf".
     */
    private fun buildOrdinal(numberLong: Long, cardinal: String): String {
        if (numberLong == 1L) return "premier"
        // Strip trailing "s" if present (e.g. "quatre-vingts" → "quatre-vingt")
        val s = if (cardinal.endsWith("s")) cardinal.dropLast(1) else cardinal
        return when {
            s.endsWith("cinq") -> s + "uième"
            s.endsWith("neuf") -> s.dropLast(1) + "vième"
            s.endsWith("e") -> s.dropLast(1) + "ième"
            else -> s + "ième"
        }
    }

    companion object {
        private val NUMBER_NAMES: Map<Long, String> = mapOf(
            0L to "zéro",
            1L to "un",
            2L to "deux",
            3L to "trois",
            4L to "quatre",
            5L to "cinq",
            6L to "six",
            7L to "sept",
            8L to "huit",
            9L to "neuf",
            10L to "dix",
            11L to "onze",
            12L to "douze",
            13L to "treize",
            14L to "quatorze",
            15L to "quinze",
            16L to "seize",
            17L to "dix-sept",
            18L to "dix-huit",
            19L to "dix-neuf",
            20L to "vingt",
            30L to "trente",
            40L to "quarante",
            50L to "cinquante",
            60L to "soixante",
            100L to "cent",
            1000L to "mille",
            1000000L to "million",
            1000000000L to "milliard",
            1000000000000L to "billion",
            1000000000000000L to "billiard",
            1000000000000000000L to "trillion",
        )

        private val ORDINAL_NAMES: Map<Long, String> = mapOf(
            1L to "premier",
            2L to "deuxième",
            3L to "troisième",
            4L to "quatrième",
            5L to "cinquième",
            6L to "sixième",
            7L to "septième",
            8L to "huitième",
            9L to "neuvième",
            10L to "dixième",
            11L to "onzième",
            12L to "douzième",
            13L to "treizième",
            14L to "quatorzième",
            15L to "quinzième",
            16L to "seizième",
            17L to "dix-septième",
            18L to "dix-huitième",
            19L to "dix-neuvième",
            1000L to "millième",
            1000000L to "millionième",
            1000000000L to "milliardième",
        )
    }
}
