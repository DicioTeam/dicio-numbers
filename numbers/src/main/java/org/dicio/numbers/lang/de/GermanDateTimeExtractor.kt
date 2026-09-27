package org.dicio.numbers.lang.de

import org.dicio.numbers.parser.lexer.TokenStream
import org.dicio.numbers.unit.Duration
import org.dicio.numbers.util.DateTimeExtractorUtils
import org.dicio.numbers.util.DurationExtractorUtils
import org.dicio.numbers.util.NumberExtractorUtils
import org.dicio.numbers.util.Utils
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

class GermanDateTimeExtractor internal constructor(
    private val ts: TokenStream,
    private val now: LocalDateTime
) {
    private val numberExtractor = GermanNumberExtractor(ts)
    private val durationExtractor = DurationExtractorUtils(ts, numberExtractor::numberNoOrdinal)
    private val dateTimeExtractor = DateTimeExtractorUtils(ts, now, this::extractIntegerInRange)

    private fun extractIntegerInRange(fromInclusive: Int, toInclusive: Int): Int? {
        return extractIntegerInRange(fromInclusive, toInclusive, false)
    }

    private fun extractIntegerInRange(
        fromInclusive: Int,
        toInclusive: Int,
        allowOrdinal: Boolean
    ): Int? {
        // disallow fraction as / should be treated as a day/month/year separator
        return NumberExtractorUtils.extractOneIntegerInRange(
            ts, fromInclusive, toInclusive
        ) { NumberExtractorUtils.signBeforeNumber(ts) { numberExtractor.numberInteger(allowOrdinal) } }
    }


    fun dateTime(): LocalDateTime? {
        // first try preferring having a date first, then try with time first
        return ts.firstWhichUsesMostTokens({ dateTime(false) }, { dateTime(true) })
    }

    private fun dateTime(timeFirst: Boolean): LocalDateTime? {
        var date: LocalDate? = null
        var time: LocalTime? = null

        if (!timeFirst) {
            // first try with special days, since duration-related words might be used
            date = relativeSpecialDay()

            if (date == null) {
                // then try with duration, since otherwise numbers would be interpreted as date days
                val duration = Utils.firstNotNull(
                    this::relativeDuration,
                    dateTimeExtractor::relativeMonthDuration
                )
                if (duration == null) {
                    // no normal relative duration found: finally try extracting a date normally
                    date = date()
                } else if (duration.nanos == 0L && duration.days != 0L) {
                    // duration contains a specified day and no specified time, so a time can follow
                    date = duration.applyAsOffsetToDateTime(now).toLocalDate()
                } else if (duration.nanos != 0L && duration.days == 0L && duration.months == 0L && duration.years == 0L) {
                    // duration contains a specified time, so a date could follow
                    time = duration.applyAsOffsetToDateTime(now).toLocalTime()
                } else {
                    // duration contains mixed date&time, or has units >=month, nothing can follow
                    return duration.applyAsOffsetToDateTime(now)
                }
            }
        }

        if (time == null) {
            time = ts.tryOrSkipDateTimeIgnore(date != null) { this.timeWithMomentOfDay() }
        }

        if (date == null && time != null) {
            // try to extract a date after the time, e.g. um drei Uhr morgen
            val originalPosition = ts.position
            val duration = ts.tryOrSkipDateTimeIgnore(true) { this.relativeDuration() }
            if (duration == null) {
                date = ts.tryOrSkipDateTimeIgnore(
                    true
                ) {
                    Utils.firstNotNull(this::relativeSpecialDay, this::date)
                }
            } else if (duration.nanos == 0L && duration.days != 0L) {
                date = duration.applyAsOffsetToDateTime(now).toLocalDate()
            } else {
                ts.position = originalPosition
            }
        }

        return if (date == null) {
            time?.atDate(now.toLocalDate())
        } else {
            if (time == null) date.atTime(now.toLocalTime()) else date.atTime(time)
        }
    }

    /**
     * A time optionally preceded or followed by a moment of the day, e.g. "um acht Uhr abends",
     * "morgens um sieben", or just a moment of the day, e.g. "abends".
     */
    fun timeWithMomentOfDay(): LocalTime? {
        var time = time()
        val pm: Boolean?
        if (time == null) {
            // if there is no time, maybe there is a moment of day preceding?
            val momentOfDay = momentOfDay() ?: return null

            time = ts.tryOrSkipDateTimeIgnore(true) { this.time() }
            if (time == null) {
                // found moment of day without a specific time
                return LocalTime.of(momentOfDay, 0)
            } else {
                // use moment of day before time to determine am/pm
                pm = DateTimeExtractorUtils.isMomentOfDayPm(momentOfDay)
            }
        } else {
            // found a time, now look for a moment of day, but "morgen" after a time is tomorrow,
            // e.g. um acht morgen
            pm = ts.tryOrSkipDateTimeIgnore(true) {
                if (ts[0].hasCategory("tomorrow")) null
                else momentOfDay()?.let(DateTimeExtractorUtils::isMomentOfDayPm)
            }
        }

        if (time.hour != 0 && pm != null) {
            // a moment of the day should not do anything after 0 (e.g. 0 Uhr abends)

            if (pm && !DateTimeExtractorUtils.isMomentOfDayPm(time.hour)) {
                // time must be in the afternoon, but time is not already, correct it
                time = time.withHour((time.hour + 12) % DateTimeExtractorUtils.HOURS_IN_DAY)
            }
        }
        return time
    }

    fun time(): LocalTime? {
        return Utils.firstNotNull(this::relativeTime, this::clockTime)
    }

    /**
     * Times relative to a full hour, e.g. "halb vier" (3:30), "viertel nach drei" (3:15),
     * "viertel vor vier" (3:45), "dreiviertel vier" (3:45), "zehn vor acht" (7:50),
     * "fünf nach halb drei" (2:35)
     */
    fun relativeTime(): LocalTime? {
        val originalPosition = ts.position
        // skip words that usually come before hours, e.g. um, gegen
        ts.movePositionForwardBy(ts.indexOfWithoutCategory("pre_hour", 0))

        val minutesFromHour: Int? = if (isHalf(ts[0])) {
            // halb vier = 3:30
            ts.movePositionForwardBy(1)
            -30
        } else if (isQuarter(ts[0]) && isQuarterAdverb(ts[1])) {
            // viertel nach drei = 3:15, viertel vor vier = 3:45
            ts.movePositionForwardBy(2)
            if (ts[-1].hasCategory("special_minute_after")) 15 else -15
        } else if (ts[0].isNumberEqualTo(3) && isQuarter(ts[1])) {
            // dreiviertel vier = 3:45
            ts.movePositionForwardBy(2)
            -15
        } else if (isQuarter(ts[0])) {
            // viertel vier = 3:15 (used in some regions)
            ts.movePositionForwardBy(1)
            -45
        } else {
            // e.g. zehn vor acht, fünf nach halb drei
            val minutes = extractIntegerInRange(1, 29)
            if (minutes != null && isQuarterAdverb(ts[0])) {
                val after = ts[0].hasCategory("special_minute_after")
                ts.movePositionForwardBy(1)
                if (isHalf(ts[0])) {
                    ts.movePositionForwardBy(1)
                    if (after) minutes - 30 else -30 - minutes
                } else {
                    if (after) minutes else -minutes
                }
            } else {
                null
            }
        }

        if (minutesFromHour == null) {
            ts.position = originalPosition
            return null
        }

        val hour = extractIntegerInRange(1, 12)
        if (hour == null) {
            ts.position = originalPosition
            return null
        }
        if (ts[0].hasCategory("post_hour")) {
            ts.movePositionForwardBy(1) // e.g. halb vier Uhr
        }

        // e.g. halb zwölf = 12:00 - 30 minutes = 11:30, halb eins = 1:00 - 30 minutes = 0:30
        return LocalTime.of(hour, 0).plusMinutes(minutesFromHour.toLong())
    }

    /**
     * e.g. "fünfzehn Uhr dreißig", "um drei", "15:30", "Mitternacht"
     */
    fun clockTime(): LocalTime? {
        // try both with a normal hour and with "Mittag"/"Mitternacht"
        val hour = Utils.firstNotNull(this::noonMidnightLike, this::hour) ?: return null
        var result = LocalTime.of(hour, 0)

        val minute = ts.tryOrSkipDateTimeIgnore(true) { dateTimeExtractor.minute() }
        if (minute == null) {
            return result
        }
        result = result.withMinute(minute)

        val second = ts.tryOrSkipDateTimeIgnore(true) { dateTimeExtractor.second() }
        if (second == null) {
            return result
        }
        return result.withSecond(second)
    }

    fun date(): LocalDate? {
        var result = now.toLocalDate()

        val dayOfWeek = dateTimeExtractor.dayOfWeek()
        val day = ts.tryOrSkipDateTimeIgnore(dayOfWeek != null) { dayOfMonth() }

        // below here, we do result.withMonth(1) because January has 31 days, so setting the
        // withDayOfMonth() below will always succeed, and then we overwrite/reset the month anyway
        val originalMonth = result.month.value // just to avoid withDayOfMonth failing
        if (day == null) {
            if (dayOfWeek != null) {
                // e.g. am Montag: the next Monday, or today if today is Monday
                val daysAhead = (dayOfWeek - result.dayOfWeek.ordinal
                        + DateTimeExtractorUtils.DAYS_IN_WEEK) % DateTimeExtractorUtils.DAYS_IN_WEEK
                return result.plus(daysAhead.toLong(), ChronoUnit.DAYS)
            }
            result = result.withMonth(1).withDayOfMonth(1)
        } else {
            result = result.withMonth(1).withDayOfMonth(day)
        }

        // do withMonth after setting the day, so it coerces the day of month within the number of
        // days in the month if needed
        // a month as number only makes sense after a day, e.g. 3.10. or dritten zehnten
        val month = ts.tryOrSkipDateTimeIgnore(day != null) {
            if (day == null) dateTimeExtractor.monthName()
            else Utils.firstNotNull(dateTimeExtractor::monthName, { extractIntegerInRange(1, 12, true) })
        }
        if (month == null) {
            if (day != null) {
                return result.withMonth(originalMonth)
            }
            // neither day nor month: do not read a lone number as a year, since e.g. "zehn" is more
            // likely an hour
            return null
        }
        result = result.withMonth(month)

        val year = ts.tryOrSkipDateTimeIgnore(true) { extractIntegerInRange(0, 999999999) }
            ?: return result

        val bcad = dateTimeExtractor.bcad()
        return result.withYear(year * (if (bcad == null || bcad) 1 else -1))
    }

    /**
     * A day of month must look like one, i.e. be an ordinal ("dritten", "3te"), be written with a
     * dot ("3."), or be followed by a month ("3 Oktober"), otherwise e.g. "zehn vor acht" would
     * contain a date.
     */
    private fun dayOfMonth(): Int? {
        val originalPosition = ts.position
        val number = numberExtractor.numberInteger(true)
        if (number == null || !number.isInteger || number.integerValue() < 1
            || number.integerValue() > 31
        ) {
            ts.position = originalPosition
            return null
        }

        if (number.isOrdinal) {
            return number.integerValue().toInt()
        } else if (NumberExtractorUtils.isRawNumber(ts[-1]) && ts[0].isValue(".")) {
            ts.movePositionForwardBy(1) // e.g. 3. Oktober, 3.10.
            return number.integerValue().toInt()
        } else if (ts[ts.indexOfWithoutCategory("date_time_ignore", 0)].hasCategory("month_name")) {
            return number.integerValue().toInt() // e.g. drei Oktober
        }

        ts.position = originalPosition
        return null
    }

    fun noonMidnightLike(): Int? {
        return noonMidnightLikeOrMomentOfDay("noon_midnight_like")
    }

    fun momentOfDay(): Int? {
        // noon_midnight_like is a part of moment_of_day, so noon and midnight are included
        return noonMidnightLikeOrMomentOfDay("moment_of_day")
    }

    private fun noonMidnightLikeOrMomentOfDay(category: String): Int? {
        val originalPosition = ts.position

        if (ts[0].hasCategory("pre_special_hour")) {
            // found a word that usually comes before special hours, e.g. diesen, um
            ts.movePositionForwardBy(1)
        }

        if (ts[0].hasCategory(category)) {
            // special hour found, e.g. Mitternacht, abends, Nachmittag
            ts.movePositionForwardBy(1)
            return ts[-1].number!!.integerValue().toInt()
        }

        // no special hour found
        ts.position = originalPosition
        return null
    }

    fun hour(): Int? {
        val originalPosition = ts.position

        // skip words that usually come before hours, e.g. um, gegen
        ts.movePositionForwardBy(ts.indexOfWithoutCategory("pre_hour", 0))

        val number = extractIntegerInRange(0, DateTimeExtractorUtils.HOURS_IN_DAY)
        if (number == null) {
            // no number found, or the number is not a valid hour, e.g. um sechsundzwanzig
            ts.position = originalPosition
            return null
        }

        if (ts[0].hasCategory("post_hour")) {
            ts.movePositionForwardBy(1) // e.g. drei Uhr
        }

        // found hour, e.g. um neunzehn Uhr
        return number % DateTimeExtractorUtils.HOURS_IN_DAY // transform 24 into 0
    }


    private fun relativeSpecialDay(): LocalDate? {
        val days = Utils.firstNotNull(
            this::relativeYesterday,
            dateTimeExtractor::relativeToday,
            this::relativeTomorrow,
            dateTimeExtractor::relativeDayOfWeekDuration
        )
        if (days == null) {
            return null
        }
        return now.toLocalDate().plusDays(days.toLong())
    }

    fun relativeYesterday(): Int? {
        val originalPosition = ts.position

        // collect as many "vor" preceding "gestern" as possible, e.g. vorvorgestern
        var dayCount = 0
        while (ts[0].hasCategory("yesterday_adder")) {
            ++dayCount
            ts.movePositionForwardBy(ts.indexOfWithoutCategory("date_time_ignore", 1))
        }

        // collect the actual "gestern" and exit if it is not found
        if (!ts[0].hasCategory("yesterday")) {
            ts.position = originalPosition
            return null
        }
        ts.movePositionForwardBy(1)
        ++dayCount

        // found relative yesterday, e.g. gestern, vorgestern
        return -dayCount
    }

    fun relativeTomorrow(): Int? {
        val originalPosition = ts.position

        // collect as many "über" preceding "morgen" as possible, e.g. überübermorgen
        var dayCount = 0
        while (ts[0].hasCategory("tomorrow_adder")) {
            ++dayCount
            ts.movePositionForwardBy(ts.indexOfWithoutCategory("date_time_ignore", 1))
        }

        // collect the actual "morgen" and exit if it is not found
        if (!ts[0].hasCategory("tomorrow")) {
            ts.position = originalPosition
            return null
        }
        ts.movePositionForwardBy(1)
        ++dayCount

        // found relative tomorrow, e.g. morgen, übermorgen
        return dayCount
    }

    fun relativeDuration(): Duration? {
        return dateTimeExtractor.relativeIndicatorDuration(
            { durationExtractor.duration() },
            { duration -> duration.multiply(-1) }
        )
    }


    private fun isHalf(token: org.dicio.numbers.parser.lexer.Token): Boolean {
        return token.hasCategory("fraction_denominator") && token.isNumberEqualTo(2)
    }

    private fun isQuarter(token: org.dicio.numbers.parser.lexer.Token): Boolean {
        return token.hasCategory("fraction_denominator") && token.isNumberEqualTo(4)
    }

    private fun isQuarterAdverb(token: org.dicio.numbers.parser.lexer.Token): Boolean {
        return token.hasCategory("special_minute_after") || token.hasCategory("special_minute_before")
    }
}
