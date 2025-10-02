package org.dicio.numbers.lang.de

import org.dicio.numbers.formatter.Formatter
import org.dicio.numbers.lang.en.EnglishFormatter
import org.dicio.numbers.unit.MixedFraction
import org.dicio.numbers.util.Utils.decimalPlacesNoFinalZeros
import org.dicio.numbers.util.Utils.longPow
import org.dicio.numbers.util.Utils.splitByModulus
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

class GermanFormatter : Formatter("config/de-de") {
    override fun niceNumber(mixedFraction: MixedFraction, speech: Boolean): String {
        if (speech) {
            val sign = if (mixedFraction.negative) "minus " else ""
            if (mixedFraction.numerator == 0) {
                return sign + pronounceNumber(mixedFraction.whole.toDouble(), 0, shortScale = true,
                                              scientific = false,
                                              ordinal = false
                )
            }

            val denominatorString: String?
            if (mixedFraction.denominator == 1) {
                denominatorString = "Eintel"
            } else if (mixedFraction.denominator == 2) {
                denominatorString = "Halbe"
            } else if (mixedFraction.denominator == 3) {
                denominatorString = "Drittel"
            } else if (mixedFraction.denominator == 7) {
                denominatorString = "Siebtel"
            } else if (mixedFraction.denominator < 20) {
                // below 20 use number name + suffix "tel"
                denominatorString = pronounceNumber(
                    mixedFraction.denominator.toDouble(),
                    0,
                    shortScale = true,
                    scientific = false,
                    ordinal = true
                ) + "tel"
            } else {
                // for 20+ use number name + suffix "stel"
                denominatorString = pronounceNumber(
                    mixedFraction.denominator.toDouble(),
                    0,
                    shortScale = true,
                    scientific = false,
                    ordinal = true
                ) + "stel"
            }
            val numeratorString = pronounceNumber(mixedFraction.numerator.toDouble(), 0,
                                                  shortScale = true,
                                                  scientific = false,
                                                  ordinal = false
            )

            return if (mixedFraction.whole == 0L) {
                "$sign$numeratorString $denominatorString"
            } else {
                (sign + pronounceNumber(
                    mixedFraction.whole.toDouble(),
                    0,
                    shortScale = true,
                    scientific = false,
                    ordinal = false
                ) + " und " + numeratorString + " " + denominatorString)
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
        var number = number
        if (number == Double.POSITIVE_INFINITY) {
            return "unendlich"
        } else if (number == Double.Companion.NEGATIVE_INFINITY) {
            return "minus unendlich"
        } else if (number.isNaN()) {
            return "keine Zahl"
        }

        // also using scientific mode if the number is too big to be spoken fully. Checking against
        // the biggest double smaller than 10^21 = 1000 * 10^18, which is the biggest pronounceable
        // number, since e.g. 999.99 * 10^18 can be pronounced correctly.
        if (scientific || abs(number) > 999999999999999934463.0) {
            val scientificFormatted = String.format(Locale.ENGLISH, "%E", number)
            val parts: Array<String?> =
                scientificFormatted.split("E".toRegex(), limit = 2).toTypedArray()
            val power = parts[1]!!.toInt().toDouble()

            if (power != 0.0) {
                // This handles negatives of powers separately from the normal
                // handling since each call disables the scientific flag
                val n = parts[0]!!.toDouble()
                return String.format(
                    "%s mal zehn hoch %s",
                    pronounceNumber(abs(n), places, shortScale,
                                    scientific = false,
                                    ordinal = false
                    ),
                    pronounceNumber(abs(power), places, shortScale,
                                    scientific = false,
                                    ordinal = false
                    )
                )
            }
        }

        val result = StringBuilder()
        if (number < 0) {
            number = -number
            // from here on number is always positive
            if (places != 0 || number >= 0.5) {
                // do not add minus if number will be rounded to 0
                result.append(if (scientific) "negative " else "minus ")
            }
        }

        val realPlaces = decimalPlacesNoFinalZeros(number, places)
        val numberIsWhole = realPlaces == 0
        // if no decimal places to be printed, numberLong should be the rounded number
        val numberLong = number.toLong() + (if (number % 1 >= 0.5 && numberIsWhole) 1 else 0)

        if (!ordinal && numberIsWhole && numberLong > 1000 && numberLong < 2000) {
            // deal with 4 digits that can be said like a date, i.e. 1972 => nineteen seventy two

            result.append(NUMBER_NAMES[numberLong / 100])
            result.append(" ")
            if (numberLong % 100 == 0L) {
                // 1900 => nineteen hundred
                result.append(NUMBER_NAMES[100L])
            } else if (numberLong % 100 < 10 && numberLong % 100 != 0L) {
                // 1906 => nineteen oh six
                result.append("oh ")
                result.append(NUMBER_NAMES[numberLong % 10])
            } else if (numberLong % 10 == 0L || numberLong % 100 < 20) {
                // 1960 => nineteen sixty; 1911 => nineteen eleven
                result.append(NUMBER_NAMES[numberLong % 100])
            } else {
                // 1961 => nineteen sixty one
                result.append(NUMBER_NAMES[numberLong % 100 - numberLong % 10])
                result.append(" ")
                result.append(NUMBER_NAMES[numberLong % 10])
            }

            return result.toString()
        }

        if (!ordinal && NUMBER_NAMES.containsKey(numberLong)) {
            if (number > 90) {
                result.append("one ")
            }
            result.append(NUMBER_NAMES[numberLong])
        } else if (shortScale) {
            var ordi = ordinal && numberIsWhole // not ordinal if not whole
            val groups = splitByModulus(numberLong, 1000)
            val groupNames: MutableList<String?> = ArrayList()
            for (i in groups.indices) {
                val z: Long = groups[i]
                if (z == 0L) {
                    continue  // skip 000 groups
                }
                var groupName = subThousand(z, i == 0 && ordi)

                if (i != 0) {
                    val magnitude = longPow(1000, i)
                    if (ordi) {
                        // ordi can be true only for the first group (i.e. at the end of the number)
                        if (z == 1L) {
                            // remove "one" from first group (e.g. "one billion, millionth")
                            groupName =
                                EnglishFormatter.Companion.ORDINAL_NAMES_SHORT_SCALE[magnitude]
                        } else {
                            groupName += " " + EnglishFormatter.Companion.ORDINAL_NAMES_SHORT_SCALE[magnitude]
                        }
                    } else {
                        groupName += " " + EnglishFormatter.Companion.NUMBER_NAMES_SHORT_SCALE[magnitude]
                    }
                }

                groupNames.add(groupName)
                ordi = false
            }

            appendSplitGroups(result, groupNames)
        } else {
            var ordi = ordinal && numberIsWhole // not ordinal if not whole
            val groups = splitByModulus(numberLong, 1000000)
            val groupNames: MutableList<String?> = ArrayList()
            for (i in groups.indices) {
                val z: Long = groups[i]
                if (z == 0L) {
                    continue  // skip 000000 groups
                }

                var groupName: String?
                if (z < 1000) {
                    groupName = subThousand(z, i == 0 && ordi)
                } else {
                    groupName = subThousand(z / 1000, false) + " thousand"
                    if (z % 1000 != 0L) {
                        groupName += (if (i == 0) ", " else " ") + subThousand(
                            z % 1000,
                            i == 0 && ordi
                        )
                    } else if (i == 0 && ordi) {
                        if (z / 1000 == 1L) {
                            groupName = "thousandth" // remove "one" from "one thousandth"
                        } else {
                            groupName += "th"
                        }
                    }
                }

                if (i != 0) {
                    val magnitude = longPow(1000000, i)
                    if (ordi) {
                        // ordi can be true only for the first group (i.e. at the end of the number)
                        if (z == 1L) {
                            // remove "one" from first group (e.g. "one billion, millionth")
                            groupName =
                                EnglishFormatter.Companion.ORDINAL_NAMES_LONG_SCALE[magnitude]
                        } else {
                            groupName += " " + EnglishFormatter.Companion.ORDINAL_NAMES_LONG_SCALE[magnitude]
                        }
                    } else {
                        groupName += " " + EnglishFormatter.Companion.NUMBER_NAMES_LONG_SCALE[magnitude]
                    }
                }

                groupNames.add(groupName)
                ordi = false
            }

            appendSplitGroups(result, groupNames)
        }

        if (realPlaces > 0) {
            if (number < 1.0 && (result.isEmpty() || "minus ".contentEquals(result))) {
                result.append("zero") // nothing was written before
            }
            result.append(" point")

            val fractionalPart = String.format("%." + realPlaces + "f", number % 1)
            for (i in 2..<fractionalPart.length) {
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
                if (time.hour < 10) {
                    result.append("zero ")
                }
                result.append(pronounceNumberDuration(time.hour.toLong()))

                result.append(" ")
                if (time.minute == 0) {
                    result.append("hundred")
                } else {
                    if (time.minute < 10) {
                        result.append("zero ")
                    }
                    result.append(pronounceNumberDuration(time.minute.toLong()))
                }

                return result.toString()
            } else {
                if (time.hour == 0 && time.minute == 0) {
                    return "midnight"
                } else if (time.hour == 12 && time.minute == 0) {
                    return "noon"
                }

                val normalizedHour = (time.hour + 11) % 12 + 1 // 1 to 12
                val result = StringBuilder()
                if (time.minute == 15) {
                    result.append("quarter past ")
                    result.append(pronounceNumberDuration(normalizedHour.toLong()))
                } else if (time.minute == 30) {
                    result.append("half past ")
                    result.append(pronounceNumberDuration(normalizedHour.toLong()))
                } else if (time.minute == 45) {
                    result.append("quarter to ")
                    result.append(pronounceNumberDuration((normalizedHour % 12 + 1).toLong()))
                } else {
                    result.append(pronounceNumberDuration(normalizedHour.toLong()))

                    if (time.minute == 0) {
                        if (!showAmPm) {
                            return "$result o'clock"
                        }
                    } else {
                        if (time.minute < 10) {
                            result.append(" oh")
                        }
                        result.append(" ")
                        result.append(pronounceNumberDuration(time.minute.toLong()))
                    }
                }

                if (showAmPm) {
                    result.append(if (time.hour >= 12) " p.m." else " a.m.")
                }
                return result.toString()
            }
        } else {
            return if (use24Hour) {
                time.format(DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH))
            } else {
                val result = time.format(
                    DateTimeFormatter.ofPattern(
                        if (showAmPm) "K:mm a" else "K:mm", Locale.ENGLISH
                    )
                )
                if (result.startsWith("0:")) {
                    "12:" + result.substring(2)
                } else {
                    result
                }
            }
        }
    }


    /**
     * @param n must be 0 <= n <= 999
     * @param ordinal whether to return an ordinal number (usually with -th)
     * @return the string representation of a number smaller than 1000
     */
    private fun subThousand(n: Long, ordinal: Boolean): String? {
        // this function calls itself inside if branches to make sure `ordinal` is respected
        if (ordinal && ORDINAL_NAMES.containsKey(n)) {
            return ORDINAL_NAMES[n]
        } else if (n < 100) {
            if (!ordinal && NUMBER_NAMES.containsKey(n)) {
                return NUMBER_NAMES[n]
            }

            // n is surely => 20 from here on, since all n < 20 are in (ORDINAL|NUMBER)_NAMES
            return (NUMBER_NAMES[n - n % 10]
                    + (if (n % 10 > 0) " " + subThousand(n % 10, ordinal) else ""))
        } else {
            return (NUMBER_NAMES[n / 100] + " hundred"
                    + (if (n % 100 > 0)
                " and " + subThousand(n % 100, ordinal)
            else
                (if (ordinal) "th" else "")))
        }
    }

    /**
     * @param result the string builder to append the comma-separated group names to
     * @param groupNames the group names
     */
    private fun appendSplitGroups(result: StringBuilder, groupNames: MutableList<String?>) {
        if (!groupNames.isEmpty()) {
            result.append(groupNames[groupNames.size - 1])
        }

        for (i in groupNames.size - 2 downTo 0) {
            result.append(", ")
            result.append(groupNames[i])
        }
    }

    companion object {
        val NUMBER_NAMES = mapOf(
            0L to "null",
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
            100L to "hundert",
            1000L to "tausend",
            1000000L to "million",
            1000000000L to "milliarde",
            1000000000000L to "billion",
            1000000000000000L to "billiarde",
            1000000000000000000L to "trillion",
        )

        val ORDINAL_NAMES = mapOf(
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
            20L to "zwanzigste",
            30L to "dreißigste",
            40L to "vierzigste",
            50L to "fünfzigste",
            60L to "sechzigste",
            70L to "siebzigste",
            80L to "achtzigste",
            90L to "neunzigste",
            100L to "hundertste",
            1000L to "tausendste",
            1000000L to "millionste",
            1000000000L to "milliardste",
            1000000000000L to "billionste",
            1000000000000000L to "billiardste",
            1000000000000000000L to "trilliardste",
        )
    }
}
