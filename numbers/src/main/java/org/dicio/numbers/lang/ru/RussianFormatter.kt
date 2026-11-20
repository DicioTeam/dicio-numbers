package org.dicio.numbers.lang.ru

import org.dicio.numbers.formatter.Formatter
import org.dicio.numbers.unit.Duration
import org.dicio.numbers.unit.MixedFraction
import org.dicio.numbers.util.Utils
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalTime
import java.time.Period
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

/**
 * Russian formatter. Russian nouns after numbers take one of three forms (одна минута, две минуты,
 * пять минут), and the numbers 1 and 2 agree in gender with the noun (один час, одна минута), so
 * this formatter overrides the duration and date functions of [Formatter], which only know about
 * singular and plural.
 */
class RussianFormatter : Formatter("config/ru-ru") {

    enum class Gender { MASCULINE, FEMININE, NEUTER }

    /** Forms of an ordinal number used by this formatter */
    private enum class OrdinalForm {
        NOMINATIVE_MASCULINE, // двадцать первый
        NOMINATIVE_FEMININE, // одна пятая
        NOMINATIVE_NEUTER, // двадцать первое (мая)
        GENITIVE_MASCULINE, // две тысячи двадцать первого (года)
        GENITIVE_PLURAL, // две пятых
    }

    override fun niceNumber(mixedFraction: MixedFraction, speech: Boolean): String {
        if (!speech) {
            return niceNumberNotSpeech(mixedFraction)
        }

        val sign = if (mixedFraction.negative) "минус " else ""
        val whole = mixedFraction.whole
        val numerator = mixedFraction.numerator.toLong()
        val denominator = mixedFraction.denominator.toLong()

        if (numerator == 0L) {
            return sign + cardinal(whole)
        }
        if (numerator == 1L && denominator == 2L) {
            // "половина" and "два с половиной" are much more natural than "одна вторая"
            return sign + if (whole == 0L) "половина" else cardinal(whole) + " с половиной"
        }

        val fraction = cardinal(numerator, Gender.FEMININE) + " " + ordinal(
            denominator,
            if (pluralForm(numerator) == 0) OrdinalForm.NOMINATIVE_FEMININE
            else OrdinalForm.GENITIVE_PLURAL
        )
        return if (whole == 0L) {
            sign + fraction
        } else {
            sign + cardinal(whole) + " и " + fraction
        }
    }

    override fun pronounceNumber(
        number: Double,
        places: Int,
        shortScale: Boolean,
        scientific: Boolean,
        ordinal: Boolean
    ): String {
        // Russian uses the short scale names (миллион, миллиард, триллион...), shortScale is
        // ignored

        if (number == Double.POSITIVE_INFINITY) {
            return "бесконечность"
        } else if (number == Double.NEGATIVE_INFINITY) {
            return "минус бесконечность"
        } else if (number.isNaN()) {
            return "не число"
        }

        // also use scientific mode if the number is too big to be spoken fully, see
        // ItalianFormatter for the explanation of the constant
        if (scientific || abs(number) > 999999999999999934463.0) {
            val scientificFormatted = String.format(Locale.ENGLISH, "%E", number)
            val parts = scientificFormatted.split("E".toRegex(), limit = 2).toTypedArray()
            val power = parts[1].toInt().toDouble()
            if (power != 0.0) {
                return pronounceNumber(parts[0].toDouble(), places, shortScale, false, false) +
                        " умножить на десять в степени " +
                        pronounceNumber(power, places, shortScale, false, false)
            }
        }

        val result = StringBuilder()
        var varNumber = number
        if (varNumber < 0) {
            varNumber = -varNumber
            if (places != 0 || varNumber >= 0.5) {
                // do not add minus if number will be rounded to 0
                result.append("минус ")
            }
        }

        val realPlaces = Utils.decimalPlacesNoFinalZeros(varNumber, places)
        if (realPlaces == 0) {
            val numberLong = Utils.roundToLong(varNumber)
            if (ordinal) {
                result.append(ordinal(numberLong, OrdinalForm.NOMINATIVE_MASCULINE))
            } else {
                result.append(cardinal(numberLong))
            }
            return result.toString()
        }

        // decimal number, e.g. "три целых четырнадцать сотых"
        val rounded = BigDecimal(varNumber).setScale(realPlaces, RoundingMode.HALF_UP)
        val wholePart = rounded.toBigInteger().toLong()
        val fractionalPart = rounded.subtract(BigDecimal(wholePart)).movePointRight(realPlaces)
            .toLong()

        if (realPlaces > DECIMAL_DENOMINATORS.size) {
            // too many decimal places to name the denominator, read the digits one by one
            result.append(cardinal(wholePart)).append(" запятая")
            val digits = fractionalPart.toString().padStart(realPlaces, '0')
            for (digit in digits) {
                result.append(" ").append(cardinal((digit - '0').toLong()))
            }
            return result.toString()
        }

        result.append(cardinal(wholePart, Gender.FEMININE))
        result.append(if (pluralForm(wholePart) == 0) " целая " else " целых ")
        result.append(cardinal(fractionalPart, Gender.FEMININE))
        result.append(" ")
        result.append(DECIMAL_DENOMINATORS[realPlaces - 1])
        result.append(if (pluralForm(fractionalPart) == 0) "ая" else "ых")
        return result.toString()
    }

    override fun pronounceNumberDuration(number: Long): String {
        return cardinal(number)
    }

    override fun niceTime(
        time: LocalTime,
        speech: Boolean,
        use24Hour: Boolean,
        showAmPm: Boolean
    ): String {
        if (speech) {
            if (!use24Hour && time.minute == 0) {
                if (time.hour == 12) {
                    return "полдень"
                } else if (time.hour == 0) {
                    return "полночь"
                }
            }

            val hour = if (use24Hour) time.hour else (time.hour % 12).let { if (it == 0) 12 else it }
            val result = StringBuilder()
            if (hour == 1 && !use24Hour) {
                result.append("час") // "час дня" is much more common than "один час дня"
            } else {
                result.append(withNoun(hour.toLong(), Gender.MASCULINE, HOUR_FORMS))
            }

            if (time.minute != 0) {
                result.append(" ")
                result.append(withNoun(time.minute.toLong(), Gender.FEMININE, MINUTE_FORMS))
            }

            if (!use24Hour && showAmPm) {
                result.append(" ").append(partOfDay(time.hour))
            }
            return result.toString()

        } else if (use24Hour) {
            return time.format(DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH))

        } else {
            val result = time.format(DateTimeFormatter.ofPattern("K:mm", Locale.ENGLISH))
                .let { if (it.startsWith("0:")) "12:" + it.substring(2) else it }
            return if (showAmPm) result + " " + partOfDay(time.hour) else result
        }
    }

    override fun niceDuration(duration: Duration, speech: Boolean): String {
        if (!speech) {
            return super.niceDuration(duration, false)
        }

        val javaDuration = duration.toJavaDuration()
        val days = javaDuration.toDays()
        val hours = javaDuration.toHours() % 24
        val minutes = javaDuration.toMinutes() % 60
        val seconds = javaDuration.seconds % 60

        val parts = ArrayList<String>()
        if (days > 0) {
            parts.add(withNoun(days, Gender.MASCULINE, DAY_FORMS))
        }
        if (hours > 0) {
            parts.add(withNoun(hours, Gender.MASCULINE, HOUR_FORMS))
        }
        if (minutes > 0) {
            parts.add(withNoun(minutes, Gender.FEMININE, MINUTE_FORMS))
        }
        // if the duration is zero also write "ноль секунд"
        if (seconds > 0 || javaDuration.seconds == 0L) {
            parts.add(withNoun(seconds, Gender.FEMININE, SECOND_FORMS))
        }
        return parts.joinToString(" ")
    }

    override fun niceDate(date: LocalDate, now: LocalDate?): String {
        var withYear = true
        var withMonth = true
        if (now != null) {
            // try to remove redundant information based on the current date, same as Formatter
            val daysDifference = Period.between(date, now).days
            if (daysDifference == 1) {
                return config.yesterday
            } else if (daysDifference == 0) {
                return config.today
            } else if (daysDifference == -1) {
                return config.tomorrow
            } else if (date.year == now.year) {
                withYear = false
                withMonth = date.month != now.month || date.dayOfMonth <= now.dayOfMonth
            }
        }

        val result = StringBuilder()
        result.append(config.weekdays[date.dayOfWeek.value - 1])
        result.append(", ")
        result.append(ordinal(date.dayOfMonth.toLong(), OrdinalForm.NOMINATIVE_NEUTER))
        if (withMonth) {
            result.append(" ").append(config.months[date.month.value - 1])
        }
        if (withYear) {
            // "две тысячи двадцать первого года"
            result.append(" ")
            result.append(ordinal(abs(date.year.toLong()), OrdinalForm.GENITIVE_MASCULINE))
            result.append(" года")
            if (date.year < 0) {
                result.append(" ").append(config.bc)
            }
        }
        return result.toString()
    }

    override fun niceYear(date: LocalDate): String {
        // years are ordinal numbers in Russian, e.g. "тысяча девятьсот восемьдесят четвёртый"
        val result = ordinal(abs(date.year.toLong()), OrdinalForm.NOMINATIVE_MASCULINE)
        return if (date.year < 0) result + " " + config.bc else result
    }


    /**
     * @return the cardinal number in the nominative case, e.g. 2 -> "два"/"две", 21000 ->
     * "двадцать одна тысяча"
     */
    fun cardinal(number: Long, gender: Gender = Gender.MASCULINE): String {
        if (number == 0L) {
            return "ноль"
        }

        val groups = Utils.splitByModulus(number, 1000)
        val parts = ArrayList<String>()
        for (i in groups.indices.reversed()) {
            val group = groups[i]
            if (group == 0L) {
                continue
            }

            if (i == 0) {
                parts.add(subThousand(group, gender))
            } else {
                val scale = SCALES[i - 1]
                if (group == 1L) {
                    parts.add(scale[0]) // "тысяча", "миллион", without "одна"/"один"
                } else {
                    // тысяча is feminine (две тысячи), the others are masculine (два миллиона)
                    val groupGender = if (i == 1) Gender.FEMININE else Gender.MASCULINE
                    parts.add(subThousand(group, groupGender) + " " + scale[pluralForm(group)])
                }
            }
        }
        return parts.joinToString(" ")
    }

    /** @return e.g. "одна минута", "две минуты", "пять минут" */
    fun withNoun(number: Long, gender: Gender, forms: Array<String>): String {
        return cardinal(number, gender) + " " + forms[pluralForm(number)]
    }

    private fun subThousand(number: Long, gender: Gender): String {
        val parts = ArrayList<String>()
        val hundreds = (number / 100).toInt()
        val rest = (number % 100).toInt()
        if (hundreds > 0) {
            parts.add(HUNDREDS[hundreds - 1])
        }
        if (rest in 10..19) {
            parts.add(TEENS[rest - 10])
        } else {
            if (rest >= 20) {
                parts.add(TENS[rest / 10 - 2])
            }
            val unit = rest % 10
            if (unit > 0) {
                parts.add(unitWord(unit, gender))
            }
        }
        return parts.joinToString(" ")
    }

    private fun unitWord(unit: Int, gender: Gender): String {
        return when {
            unit == 1 && gender == Gender.FEMININE -> "одна"
            unit == 1 && gender == Gender.NEUTER -> "одно"
            unit == 2 && gender == Gender.FEMININE -> "две"
            else -> UNITS[unit - 1]
        }
    }

    /**
     * @return the ordinal number in the given form, e.g. 21 -> "двадцать первый", 2000 ->
     * "двухтысячный", 1021 -> "тысяча двадцать первый"
     */
    private fun ordinal(number: Long, form: OrdinalForm): String {
        val words = ordinalNominativeMasculine(number)
        val lastSpace = words.lastIndexOf(' ')
        return words.substring(0, lastSpace + 1) + inflectOrdinal(words.substring(lastSpace + 1), form)
    }

    private fun ordinalNominativeMasculine(number: Long): String {
        if (number == 0L) {
            return "нулевой"
        }

        val groups = Utils.splitByModulus(number, 1000)
        val lowest = groups.indexOfFirst { it != 0L }
        val lowestMagnitude = Utils.longPow(1000, lowest + 1)
        // everything above the lowest non-zero group is said as a cardinal number
        val prefix = (number / lowestMagnitude * lowestMagnitude).let {
            if (it == 0L) "" else cardinal(it) + " "
        }

        val group = groups[lowest]
        if (lowest == 0) {
            return prefix + subThousandOrdinal(group)
        }

        // the ordinal is a compound word, e.g. двухтысячный, трёхсотмиллионный, тысячный
        val compound = if (group == 1L) "" else genitiveCompound(group)
        return prefix + compound + ORDINAL_SCALES[lowest - 1]
    }

    private fun subThousandOrdinal(number: Long): String {
        val hundreds = (number / 100).toInt()
        val rest = (number % 100).toInt()
        val tens = rest / 10
        val unit = rest % 10

        if (rest == 0) {
            return ORDINAL_HUNDREDS[hundreds - 1] // e.g. сотый, двухсотый
        }

        val prefix = StringBuilder()
        if (hundreds > 0) {
            prefix.append(HUNDREDS[hundreds - 1]).append(" ")
        }
        return if (rest in 10..19) {
            prefix.append(ORDINAL_TEENS[rest - 10]).toString()
        } else if (unit == 0) {
            prefix.append(ORDINAL_TENS[tens - 2]).toString()
        } else {
            if (tens >= 2) {
                prefix.append(TENS[tens - 2]).append(" ")
            }
            prefix.append(ORDINAL_UNITS[unit - 1]).toString()
        }
    }

    /** @return the genitive form used in compound ordinals, e.g. 2 -> двух, 25 -> двадцатипяти */
    private fun genitiveCompound(number: Long): String {
        val hundreds = (number / 100).toInt()
        val rest = (number % 100).toInt()
        val result = StringBuilder()
        if (hundreds > 0) {
            result.append(GENITIVE_HUNDREDS[hundreds - 1])
        }
        if (rest in 10..19) {
            result.append(GENITIVE_TEENS[rest - 10])
        } else {
            if (rest >= 20) {
                result.append(GENITIVE_TENS[rest / 10 - 2])
            }
            if (rest % 10 > 0) {
                result.append(GENITIVE_UNITS[rest % 10 - 1])
            }
        }
        return result.toString()
    }

    private fun inflectOrdinal(word: String, form: OrdinalForm): String {
        if (form == OrdinalForm.NOMINATIVE_MASCULINE) {
            return word
        }
        if (word.endsWith("ий")) {
            // третий has a soft stem: третья, третье, третьего, третьих
            val stem = word.substring(0, word.length - 2) + "ь"
            return stem + when (form) {
                OrdinalForm.NOMINATIVE_FEMININE -> "я"
                OrdinalForm.NOMINATIVE_NEUTER -> "е"
                OrdinalForm.GENITIVE_MASCULINE -> "его"
                else -> "их"
            }
        }
        // all other ordinals end with -ый or -ой
        val stem = word.substring(0, word.length - 2)
        return stem + when (form) {
            OrdinalForm.NOMINATIVE_FEMININE -> "ая"
            OrdinalForm.NOMINATIVE_NEUTER -> "ое"
            OrdinalForm.GENITIVE_MASCULINE -> "ого"
            else -> "ых"
        }
    }

    private fun partOfDay(hour: Int): String {
        return when (hour) {
            in 0..3 -> "ночи"
            in 4..11 -> "утра"
            in 12..16 -> "дня"
            else -> "вечера"
        }
    }

    companion object {
        /**
         * @return the index of the noun form to use after the number: 0 for e.g. одна минута,
         * 1 for e.g. две минуты, 2 for e.g. пять минут
         */
        @JvmStatic
        fun pluralForm(number: Long): Int {
            val n = abs(number)
            return if (n % 10 == 1L && n % 100 != 11L) {
                0
            } else if (n % 10 in 2L..4L && n % 100 !in 12L..14L) {
                1
            } else {
                2
            }
        }

        private val UNITS = arrayOf(
            "один", "два", "три", "четыре", "пять", "шесть", "семь", "восемь", "девять"
        )
        private val TEENS = arrayOf(
            "десять", "одиннадцать", "двенадцать", "тринадцать", "четырнадцать", "пятнадцать",
            "шестнадцать", "семнадцать", "восемнадцать", "девятнадцать"
        )
        private val TENS = arrayOf(
            "двадцать", "тридцать", "сорок", "пятьдесят", "шестьдесят", "семьдесят",
            "восемьдесят", "девяносто"
        )
        private val HUNDREDS = arrayOf(
            "сто", "двести", "триста", "четыреста", "пятьсот", "шестьсот", "семьсот",
            "восемьсот", "девятьсот"
        )
        private val SCALES = arrayOf(
            arrayOf("тысяча", "тысячи", "тысяч"),
            arrayOf("миллион", "миллиона", "миллионов"),
            arrayOf("миллиард", "миллиарда", "миллиардов"),
            arrayOf("триллион", "триллиона", "триллионов"),
            arrayOf("квадриллион", "квадриллиона", "квадриллионов"),
            arrayOf("квинтиллион", "квинтиллиона", "квинтиллионов"),
        )

        private val ORDINAL_UNITS = arrayOf(
            "первый", "второй", "третий", "четвёртый", "пятый", "шестой", "седьмой", "восьмой",
            "девятый"
        )
        private val ORDINAL_TEENS = arrayOf(
            "десятый", "одиннадцатый", "двенадцатый", "тринадцатый", "четырнадцатый",
            "пятнадцатый", "шестнадцатый", "семнадцатый", "восемнадцатый", "девятнадцатый"
        )
        private val ORDINAL_TENS = arrayOf(
            "двадцатый", "тридцатый", "сороковой", "пятидесятый", "шестидесятый",
            "семидесятый", "восьмидесятый", "девяностый"
        )
        private val ORDINAL_HUNDREDS = arrayOf(
            "сотый", "двухсотый", "трёхсотый", "четырёхсотый", "пятисотый", "шестисотый",
            "семисотый", "восьмисотый", "девятисотый"
        )
        private val ORDINAL_SCALES = arrayOf(
            "тысячный", "миллионный", "миллиардный", "триллионный", "квадриллионный",
            "квинтиллионный"
        )

        private val GENITIVE_UNITS = arrayOf(
            "одно", "двух", "трёх", "четырёх", "пяти", "шести", "семи", "восьми", "девяти"
        )
        private val GENITIVE_TEENS = arrayOf(
            "десяти", "одиннадцати", "двенадцати", "тринадцати", "четырнадцати", "пятнадцати",
            "шестнадцати", "семнадцати", "восемнадцати", "девятнадцати"
        )
        private val GENITIVE_TENS = arrayOf(
            "двадцати", "тридцати", "сорока", "пятидесяти", "шестидесяти", "семидесяти",
            "восьмидесяти", "девяноста"
        )
        private val GENITIVE_HUNDREDS = arrayOf(
            "сто", "двухсот", "трёхсот", "четырёхсот", "пятисот", "шестисот", "семисот",
            "восьмисот", "девятисот"
        )

        /** Stems of the names of decimal fractions: десятая, сотая, тысячная... */
        private val DECIMAL_DENOMINATORS = arrayOf(
            "десят", "сот", "тысячн", "десятитысячн", "стотысячн", "миллионн",
            "десятимиллионн", "стомиллионн", "миллиардн"
        )

        val DAY_FORMS = arrayOf("день", "дня", "дней")
        val HOUR_FORMS = arrayOf("час", "часа", "часов")
        val MINUTE_FORMS = arrayOf("минута", "минуты", "минут")
        val SECOND_FORMS = arrayOf("секунда", "секунды", "секунд")
    }
}
