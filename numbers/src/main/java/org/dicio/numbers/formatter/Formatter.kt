package org.dicio.numbers.formatter

import org.dicio.numbers.formatter.datetime.DateTimeConfig
import org.dicio.numbers.formatter.datetime.NiceYearSubstitutionTableBuilder
import org.dicio.numbers.unit.Duration
import org.dicio.numbers.unit.MixedFraction
import org.dicio.numbers.util.Utils
import java.time.LocalDate
import java.time.LocalTime
import java.time.Period
import java.time.temporal.ChronoUnit
import kotlin.math.abs

abstract class Formatter protected constructor(configFolder: String) {
    protected val config: DateTimeConfig = DateTimeConfig(configFolder)


    /**
     * Format a mixed fraction to a human readable representation. For example, 5 + 3/4 would be
     * formatted into "five and three quarters" for English.
     *
     * @param mixedFraction the mixed fraction to format
     * @param speech format for speech (true) or display (false)
     * @return the formatted mixed fraction as a string
     */
    abstract fun niceNumber(mixedFraction: MixedFraction, speech: Boolean): String

    protected fun niceNumberNotSpeech(mixedFraction: MixedFraction): String {
        val sign = if (mixedFraction.negative) "-" else ""
        return if (mixedFraction.numerator == 0) {
            sign + mixedFraction.whole
        } else if (mixedFraction.whole == 0L) {
            sign + mixedFraction.numerator + "/" + mixedFraction.denominator
        } else {
            (sign + mixedFraction.whole + " "
                    + mixedFraction.numerator + "/" + mixedFraction.denominator)
        }
    }

    /**
     * Format a number to a pronounceable representation. For example, -4000619 would be formatted
     * into "minus four million, six hundred and nineteen" for English.
     *
     * @param number the number to pronounce
     * @param places the number of decimal places to round decimal numbers to
     * @param shortScale use short (true) or long (false) scale for large numbers (see
     * [
 * Names of large numbers](https://en.wikipedia.org/wiki/Names_of_large_numbers))
     * @param scientific if true convert and pronounce in scientific notation
     * @param ordinal if true pronounce in the ordinal form (e.g. "first" instead of "one" for
     * English)
     * @return the formatted number as a string
     */
    abstract fun pronounceNumber(
        number: Double,
        places: Int,
        shortScale: Boolean,
        scientific: Boolean,
        ordinal: Boolean
    ): String

    /**
     * Format a date to a pronounceable representation. For example, 2021/4/28 would be formatted
     * as "wednesday, april twenty-eighth, twenty twenty one" for English.
     *
     * @param date the date to format (assumes already in local timezone)
     * @param now the current date or null. If not null, the returned date for speech will be
     * shortened accordingly: no year is returned if now is in the same year as date, no
     * month is returned if now is in the same year and in the same month as date.
     * Yesterday, today and tomorrow translations are also used.
     * @return the formatted date string
     */
    fun niceDate(date: LocalDate, now: LocalDate?): String {
        var formatString = config.dateFormatFull
        if (now != null) {
            // try to remove redundant information based on the current date
            val daysDifference = Period.between(date, now).days
            if (daysDifference == 1) {
                return config.yesterday
            } else if (daysDifference == 0) {
                return config.today
            } else if (daysDifference == -1) {
                return config.tomorrow
            } else if (date.year == now.year) {
                formatString = if (date.month == now.month
                    && date.dayOfMonth > now.dayOfMonth
                ) {
                    config.dateFormatFullNoYearMonth
                } else {
                    config.dateFormatFullNoYear
                }
            }
        }

        return formatString.format(mapOf(
            "day" to config.days[date.dayOfMonth - 1],
            "weekday" to config.weekdays[date.dayOfWeek.value - 1],
            "month" to config.months[date.month.value - 1],
            "formatted_year" to niceYear(date),
        ))
    }

    /**
     * Format the year from a date to a pronounceable year. For example, year 1984 would be
     * formatted as "nineteen eighty four" for English.
     *
     * @param date the date containing the year to format (assumes already in local timezone)
     * @return the formatted year string
     */
    fun niceYear(date: LocalDate): String {
        val substitutionTable = NiceYearSubstitutionTableBuilder.build(config, date.year)
        val year = abs(date.year.toDouble()).toInt()

        substitutionTable["number"] = (year % 100).toString()
        substitutionTable["formatted_decade"] = config.decadeFormat
            .getMostSuitableFormatString(year % 100).format(substitutionTable)

        substitutionTable["number"] = (year % 1000).toString()
        substitutionTable["formatted_hundreds"] = config.hundredFormat
            .getMostSuitableFormatString(year % 1000).format(substitutionTable)

        substitutionTable["number"] = (year % 10000).toString()
        substitutionTable["formatted_thousand"] = config.thousandFormat
            .getMostSuitableFormatString(year % 10000).format(substitutionTable)

        substitutionTable["number"] = year.toString()
        substitutionTable["bc"] = if (date.year >= 0) "" else config.bc

        val formattedYear =
            config.yearFormat.getMostSuitableFormatString(year).format(substitutionTable)
        return Utils.removeRedundantSpaces(formattedYear)
    }

    /**
     * Format a time to a human readable representation. For example, 5:30 would be formatted as
     * "five thirty" for English.
     *
     * @param time the time to format (assumes already in local timezone)
     * @param speech format for speech (true) or display (false)
     * @param use24Hour output in 24-hour/military (true) or 12-hour (false) format
     * @param showAmPm if true include the am/pm for 12-hour format
     * @return the formatted time string
     */
    abstract fun niceTime(
        time: LocalTime,
        speech: Boolean,
        use24Hour: Boolean,
        showAmPm: Boolean
    ): String

    /**
     * Format a date time to a pronounceable date and time. For example, 2021/4/28 5:30 would be
     * formatted as "wednesday, april twenty-eighth, twenty twenty one at five thirty" for English.
     *
     * @param date the date to format (assumes already in local timezone)
     * @param now the current date or null. If not null, the returned date for speech will be
     * shortened accordingly: no year is returned if now is in the same year as date, no
     * month is returned if now is in the same year and in the same month as date.
     * Yesterday, today and tomorrow translations are also used.
     * @param time the time to format (assumes already in local timezone)
     * @param use24Hour output in 24-hour/military (true) or 12-hour (false) format
     * @param showAmPm if true include the am/pm for 12-hour format
     * @return the formatted date time string
     */
    fun niceDateTime(
        date: LocalDate,
        now: LocalDate?,
        time: LocalTime,
        use24Hour: Boolean,
        showAmPm: Boolean
    ): String {
        return config.dateTimeFormat.format(mapOf(
            "formatted_date" to niceDate(date, now),
            "formatted_time" to niceTime(time, true, use24Hour, showAmPm),
        ))
    }

    /**
     * Format a duration to a human readable representation. For example, 12 days 3:23:01 would be
     * formatted as "twelve days three hours twenty three minutes one second".
     *
     * @param duration the duration to format
     * @param speech format for speech (true) or display (false)
     * @return the formatted time span string
     */
    fun niceDuration(duration: Duration, speech: Boolean): String {
        val javaDuration = duration.toJavaDuration()
        val days = javaDuration.toDays()
        val hours = javaDuration.toHours() % 24
        val minutes = javaDuration.toMinutes() % 60
        val seconds = javaDuration.seconds % 60

        val result = StringBuilder()
        if (speech) {
            if (days > 0) {
                result.append(pronounceNumberDuration(days, ChronoUnit.DAYS))
                result.append(" ")
                result.append(if (days == 1L) config.dayWord else config.daysWord)
            }

            if (hours > 0) {
                if (result.isNotEmpty()) {
                    result.append(" ")
                }
                result.append(pronounceNumberDuration(hours, ChronoUnit.HOURS))
                result.append(" ")
                result.append(if (hours == 1L) config.hourWord else config.hoursWord)
            }

            if (minutes > 0) {
                if (result.isNotEmpty()) {
                    result.append(" ")
                }
                result.append(pronounceNumberDuration(minutes, ChronoUnit.MINUTES))
                result.append(" ")
                result.append(if (minutes == 1L) config.minuteWord else config.minutesWord)
            }

            // if the duration is zero also write "zero seconds"
            if (seconds > 0 || javaDuration.seconds == 0L) {
                if (result.isNotEmpty()) {
                    result.append(" ")
                }
                result.append(pronounceNumberDuration(seconds, ChronoUnit.SECONDS))
                result.append(" ")
                result.append(if (seconds == 1L) config.secondWord else config.secondsWord)
            }
        } else {
            if (days > 0) {
                result.append(days)
                result.append("d ")
            }

            if (hours > 0 || days > 0) {
                result.append(hours)
                result.append(":")
            }

            if (minutes < 10 && (hours > 0 || days > 0)) {
                result.append("0")
            }
            result.append(minutes)
            result.append(":")

            if (seconds < 10) {
                result.append("0")
            }
            result.append(seconds)
        }

        return result.toString()
    }

    /**
     * Pronounces the amount of a unit in a duration, e.g. "twelve" in "twelve days". Languages in
     * which the number word depends on the unit can override this, e.g. German "ein Tag" but
     * "eine Stunde".
     */
    protected open fun pronounceNumberDuration(number: Long, unit: ChronoUnit): String {
        return pronounceNumberDuration(number)
    }

    protected open fun pronounceNumberDuration(number: Long): String {
        return pronounceNumber(number.toDouble(), 0, true, false, false)
    }
}
