package org.dicio.numbers.lang.de

import org.dicio.numbers.formatter.Formatter
import org.dicio.numbers.unit.MixedFraction
import org.dicio.numbers.util.Utils.decimalPlacesNoFinalZeros
import org.dicio.numbers.util.Utils.splitByModulus
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.abs

/**
 * German spells numbers below one million as a single word with the units before the tens, e.g.
 * 243 = "zweihundertdreiundvierzig", while Million, Milliarde, ... are separate nouns. German
 * always uses the long scale, so the shortScale parameter is ignored.
 */
class GermanFormatter : Formatter("config/de-de") {
    override fun niceNumber(mixedFraction: MixedFraction, speech: Boolean): String {
        if (!speech) {
            return niceNumberNotSpeech(mixedFraction)
        }

        val sign = if (mixedFraction.negative) "minus " else ""
        if (mixedFraction.numerator == 0) {
            return sign + cardinal(mixedFraction.whole, true)
        }

        val fraction = if (mixedFraction.numerator == 1 && mixedFraction.denominator == 2) {
            "einhalb"
        } else {
            (if (mixedFraction.numerator == 1) "ein" else cardinal(mixedFraction.numerator.toLong(), true)) +
                    " " + denominatorName(mixedFraction.denominator.toLong())
        }

        return if (mixedFraction.whole == 0L) {
            sign + fraction
        } else {
            sign + cardinal(mixedFraction.whole, true) + " und " + fraction
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
            return "unendlich"
        } else if (number == Double.NEGATIVE_INFINITY) {
            return "minus unendlich"
        } else if (number.isNaN()) {
            return "keine Zahl"
        }

        // also using scientific mode if the number is too big to be spoken fully. Checking against
        // the biggest double smaller than 10^21 = 1000 * 10^18, which is the biggest pronounceable
        // number, since e.g. 999.99 * 10^18 can be pronounced correctly.
        if (scientific || abs(number) > 999999999999999934463.0) {
            val scientificFormatted = String.format(Locale.ENGLISH, "%E", number)
            val parts = scientificFormatted.split("E".toRegex(), limit = 2).toTypedArray()
            val power = parts[1].toInt().toDouble()

            if (power != 0.0) {
                // This handles negatives of powers separately from the normal
                // handling since each call disables the scientific flag
                val n = parts[0].toDouble()
                return String.format(
                    "%s%s mal zehn hoch %s%s",
                    if (n < 0) "minus " else "",
                    pronounceNumber(abs(n), places, shortScale, scientific = false, ordinal = false),
                    if (power < 0) "minus " else "",
                    pronounceNumber(abs(power), places, shortScale, scientific = false, ordinal = false)
                )
            }
        }

        val result = StringBuilder()
        var positive = number
        if (number < 0) {
            positive = -number
            if (places != 0 || positive >= 0.5) {
                // do not add minus if number will be rounded to 0
                result.append("minus ")
            }
        }

        val realPlaces = decimalPlacesNoFinalZeros(positive, places)
        val numberIsWhole = realPlaces == 0
        // if no decimal places to be printed, numberLong should be the rounded number
        val numberLong = positive.toLong() + (if (positive % 1 >= 0.5 && numberIsWhole) 1 else 0)

        if (ordinal && numberIsWhole) {
            result.append(ordinal(numberLong))
        } else {
            result.append(cardinal(numberLong, true))
        }

        if (realPlaces > 0) {
            result.append(" komma")
            val fractionalPart = String.format(Locale.ENGLISH, "%." + realPlaces + "f", positive % 1)
            for (i in 2 until fractionalPart.length) {
                result.append(" ")
                result.append(cardinal((fractionalPart[i].code - '0'.code).toLong(), true))
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
        if (!speech) {
            return if (use24Hour) {
                time.format(DateTimeFormatter.ofPattern("HH:mm", Locale.GERMAN))
            } else {
                val result = time.format(
                    DateTimeFormatter.ofPattern(if (showAmPm) "K:mm a" else "K:mm", Locale.ENGLISH)
                )
                if (result.startsWith("0:")) "12:" + result.substring(2) else result
            }
        }

        if (use24Hour) {
            // e.g. dreizehn Uhr zweiundzwanzig, ein Uhr, null Uhr fünf
            val result = StringBuilder(hourBeforeUhr(time.hour.toLong()))
            result.append(" Uhr")
            if (time.minute != 0) {
                result.append(" ")
                result.append(cardinal(time.minute.toLong(), true))
            }
            return result.toString()
        }

        if (time.hour == 0 && time.minute == 0) {
            return "Mitternacht"
        } else if (time.hour == 12 && time.minute == 0) {
            return "zwölf Uhr mittags"
        }

        val hour = (time.hour + 11) % 12 + 1 // 1 to 12
        val nextHour = hour % 12 + 1
        val result = StringBuilder()
        when (time.minute) {
            0 -> result.append(hourBeforeUhr(hour.toLong())).append(" Uhr")
            15 -> result.append("viertel nach ").append(cardinal(hour.toLong(), true))
            30 -> result.append("halb ").append(cardinal(nextHour.toLong(), true))
            45 -> result.append("viertel vor ").append(cardinal(nextHour.toLong(), true))
            else -> result.append(hourBeforeUhr(hour.toLong())).append(" Uhr ")
                .append(cardinal(time.minute.toLong(), true))
        }

        if (showAmPm) {
            result.append(" ")
            result.append(
                when (time.hour) {
                    in 5..9 -> "morgens"
                    in 10..11 -> "vormittags"
                    in 12..17 -> "nachmittags"
                    in 18..21 -> "abends"
                    else -> "nachts"
                }
            )
        }
        return result.toString()
    }

    override fun pronounceNumberDuration(number: Long, unit: ChronoUnit): String {
        if (number == 1L) {
            // "ein Tag" (masculine), but "eine Stunde", "eine Minute", "eine Sekunde" (feminine)
            return if (unit == ChronoUnit.DAYS) "ein" else "eine"
        }
        return pronounceNumberDuration(number)
    }


    /** "ein Uhr" instead of "eins Uhr" */
    private fun hourBeforeUhr(hour: Long): String {
        return if (hour == 1L) "ein" else cardinal(hour, true)
    }

    /** e.g. 3 -> "Drittel", 20 -> "Zwanzigstel", 1000000 -> "Millionstel" */
    private fun denominatorName(denominator: Long): String {
        return (ordinal(denominator) + "l").replaceFirstChar { it.uppercaseChar() }
    }

    /**
     * @param n the non-negative number to pronounce
     * @param isFinal whether nothing follows the number, so that 1 at the end is "eins" (e.g.
     * "hunderteins") instead of "ein" (e.g. "einhundert", "ein Uhr")
     * @return the cardinal number, e.g. "zwei Millionen dreihunderttausendvierzig"
     */
    private fun cardinal(n: Long, isFinal: Boolean): String {
        if (n == 0L) {
            return "null"
        }

        val groups = splitByModulus(n, 1000) // groups[0] = units, groups[1] = thousands, ...
        val bigParts = ArrayList<String>() // million and above, separate words
        for (i in groups.size - 1 downTo 2) {
            val group = groups[i]
            if (group == 0L) {
                continue
            }
            val (singular, plural) = BIG_NUMBER_NAMES[i]
            bigParts.add(
                if (group == 1L) "eine $singular"
                else subThousand(group, false) + " " + plural
            )
        }

        // below one million everything is a single word
        val smallPart = StringBuilder()
        if (groups.size > 1 && groups[1] != 0L) {
            smallPart.append(if (groups[1] == 1L) "ein" else subThousand(groups[1], false))
            smallPart.append("tausend")
        }
        if (groups[0] != 0L) {
            smallPart.append(subThousand(groups[0], isFinal))
        }

        if (smallPart.isNotEmpty()) {
            bigParts.add(smallPart.toString())
        }
        return bigParts.joinToString(" ")
    }

    /**
     * @param n must be 0 < n <= 999
     */
    private fun subThousand(n: Long, isFinal: Boolean): String {
        val result = StringBuilder()
        val hundreds = n / 100
        val rest = n % 100
        if (hundreds > 0) {
            result.append(if (hundreds == 1L) "ein" else NUMBER_NAMES[hundreds])
            result.append("hundert")
        }

        if (rest == 1L) {
            result.append(if (isFinal) "eins" else "ein")
        } else if (rest in 2..19) {
            result.append(NUMBER_NAMES[rest])
        } else if (rest >= 20) {
            val units = rest % 10
            if (units != 0L) {
                // e.g. dreiundzwanzig
                result.append(if (units == 1L) "ein" else NUMBER_NAMES[units])
                result.append("und")
            }
            result.append(NUMBER_NAMES[rest - units])
        }
        return result.toString()
    }

    /**
     * @param n the non-negative number to pronounce as ordinal
     * @return e.g. "dritte", "einundzwanzigste", "hundertzweite", "zweimillionste"
     */
    private fun ordinal(n: Long): String {
        ORDINAL_NAMES[n]?.let { return it }

        val lastTwoDigits = n % 100
        if (lastTwoDigits in 1..19) {
            // e.g. hundertzweite, tausendelfte
            val prefix = cardinal(n - lastTwoDigits, false)
            val separator = if ((n - lastTwoDigits) % 1000000 == 0L) " " else ""
            return prefix + separator + ORDINAL_NAMES[lastTwoDigits]
        }

        val groups = splitByModulus(n, 1000)
        if (groups.size > 2 && groups[0] == 0L && groups[1] == 0L) {
            // ends with a round million, milliard, ...: the last group becomes one word with the
            // singular, e.g. zweimillionste, zwei Milliarden dreimillionste
            val lowest = groups.indexOfFirst { it != 0L }
            val group = groups[lowest]
            val higher = n - group * BIG_NUMBER_VALUES[lowest]
            return (if (higher == 0L) "" else cardinal(higher, false) + " ") +
                    (if (group == 1L) "" else subThousand(group, false)) +
                    BIG_NUMBER_NAMES[lowest].first.lowercase() + "ste"
        }

        // e.g. zwanzigste, einundzwanzigste, hundertste, dreitausendste
        return cardinal(n, false) + "ste"
    }

    companion object {
        private val NUMBER_NAMES = mapOf(
            1L to "eins",
            2L to "zwei",
            3L to "drei",
            4L to "vier",
            5L to "fünf",
            6L to "sechs",
            7L to "sieben",
            8L to "acht",
            9L to "neun",
            10L to "zehn",
            11L to "elf",
            12L to "zwölf",
            13L to "dreizehn",
            14L to "vierzehn",
            15L to "fünfzehn",
            16L to "sechzehn",
            17L to "siebzehn",
            18L to "achtzehn",
            19L to "neunzehn",
            20L to "zwanzig",
            30L to "dreißig",
            40L to "vierzig",
            50L to "fünfzig",
            60L to "sechzig",
            70L to "siebzig",
            80L to "achtzig",
            90L to "neunzig",
        )

        private val ORDINAL_NAMES = mapOf(
            0L to "nullte",
            1L to "erste",
            2L to "zweite",
            3L to "dritte",
            4L to "vierte",
            5L to "fünfte",
            6L to "sechste",
            7L to "siebte",
            8L to "achte",
            9L to "neunte",
            10L to "zehnte",
            11L to "elfte",
            12L to "zwölfte",
            13L to "dreizehnte",
            14L to "vierzehnte",
            15L to "fünfzehnte",
            16L to "sechzehnte",
            17L to "siebzehnte",
            18L to "achtzehnte",
            19L to "neunzehnte",
            100L to "hundertste",
            1000L to "tausendste",
        )

        private val BIG_NUMBER_VALUES = listOf(
            1L, 1000L, 1000000L, 1000000000L, 1000000000000L, 1000000000000000L,
            1000000000000000000L,
        )

        /** singular and plural name of 1000^i, only for i >= 2 */
        private val BIG_NUMBER_NAMES = listOf(
            Pair("", ""),
            Pair("", ""),
            Pair("Million", "Millionen"),
            Pair("Milliarde", "Milliarden"),
            Pair("Billion", "Billionen"),
            Pair("Billiarde", "Billiarden"),
            Pair("Trillion", "Trillionen"),
        )
    }
}
