package org.dicio.numbers.lang.es

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

class SpanishDateTimeExtractor internal constructor(
    private val ts: TokenStream,
    private val preferMonthBeforeDay: Boolean, // Added parameter to match English functionality
    private val now: LocalDateTime
) {
    private val numberExtractor = SpanishNumberExtractor(ts)
    private val durationExtractor = DurationExtractorUtils(ts, numberExtractor::numberNoOrdinal)
    private val dateTimeExtractor = DateTimeExtractorUtils(ts, now, this::extractIntegerInRange)

    private fun extractIntegerInRange(fromInclusive: Int, toInclusive: Int, allowOrdinal: Boolean = false): Int? {
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
                val duration = Utils.firstNotNull(this::relativeDuration, dateTimeExtractor::relativeMonthDuration)
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
            time = ts.tryOrSkipDateTimeIgnore(date != null) { this.timeWithAmpm() }
        }

        if (date == null && time != null) {
            // try to extract a date after the time
            val originalPosition = ts.position
            val duration = ts.tryOrSkipDateTimeIgnore(true) { this.relativeDuration() }
            if (duration == null) {
                date = ts.tryOrSkipDateTimeIgnore(true) { Utils.firstNotNull(this::relativeSpecialDay, this::date) }
            } else if (duration.nanos == 0L && duration.days != 0L) {
                date = duration.applyAsOffsetToDateTime(now).toLocalDate()
            } else {
                ts.position = originalPosition
            }
        }

        return if (date == null) {
            time?.atDate(now.toLocalDate())
        } else {
            time?.let { date.atTime(it) } ?: date.atTime(now.toLocalTime())
        }
    }

    fun timeWithAmpm(): LocalTime? {
        var time = time()
        val pm: Boolean?
        if (time == null) {
            // if there is no time, maybe there is a moment of day (not am/pm though) preceding?
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
            // found a time, now look for am/pm or a moment of day
            pm = ts.tryOrSkipDateTimeIgnore(true) {
                Utils.firstNotNull(
                    dateTimeExtractor::ampm,
                    { momentOfDay()?.let(DateTimeExtractorUtils::isMomentOfDayPm) }
                )
            }
        }

        if (time.hour != 0 && pm != null) {
            // AM/PM should not do anything after 0 (e.g. 0pm)
            if (!pm && time.hour == 12) {
                // Spanish context: 12 am is midnight
                time = time.withHour(0)
            } else if (pm && !DateTimeExtractorUtils.isMomentOfDayPm(time.hour)) {
                // time must be in the afternoon, but time is not already, correct it
                time = time.withHour((time.hour + 12) % DateTimeExtractorUtils.HOURS_IN_DAY)
            }
        }
        return time
    }

    fun time(): LocalTime? {
        val originalPosition = ts.position
        val specialMinute = specialMinute() // e.g., "y cuarto", "menos cuarto"

        val hour = Utils.firstNotNull(this::noonMidnightLike, this::hour)
        if (hour == null) {
            ts.position = originalPosition
            return null
        }
        
        if (specialMinute != null) {
            // Logic for phrases like "seis menos cuarto" (quarter to six)
            return if (specialMinute < 0) {
                LocalTime.of((hour + 23) % 24, 60 + specialMinute)
            } else {
                // Logic for "seis y cuarto" (quarter past six) or "seis y media" (half past six)
                LocalTime.of(hour, specialMinute)
            }
        }
        
        var result = LocalTime.of(hour, 0)

        // Handle "en punto" (o'clock)
        if (ts[0].isValue("en") && ts[1].isValue("punto")) {
            ts.movePositionForwardBy(2)
            return result
        }

        val minute = ts.tryOrSkipDateTimeIgnore(true) { dateTimeExtractor.minute() }
        if (minute == null) return result
        
        result = result.withMinute(minute)
        val second = ts.tryOrSkipDateTimeIgnore(true) { dateTimeExtractor.second() }
        
        return second?.let { result.withSecond(it) } ?: result
    }

    fun date(): LocalDate? {
        var result = now.toLocalDate()

        val dayOfWeek = dateTimeExtractor.dayOfWeek()
        val firstNum = ts.tryOrSkipDateTimeIgnore(dayOfWeek != null) { extractIntegerInRange(1, 31, true) }

        if (firstNum == null && dayOfWeek != null) {
            // e.g. "próximo martes" (next Tuesday)
            // TODO maybe enforce the date to be in the future?
            return result.plus((dayOfWeek - result.dayOfWeek.ordinal).toLong(), ChronoUnit.DAYS)
        }

        val monthName = ts.tryOrSkipDateTimeIgnore(firstNum != null) { dateTimeExtractor.monthName() }
        if (monthName == null) {
            // Date format is likely number-based, e.g., 25/12/2023
            result = if (firstNum == null) {
                result.withDayOfMonth(1).withMonth(1)
            } else {
                val secondNumMax = if (firstNum <= 12) 31 else 12
                val secondNum = ts.tryOrSkipDateTimeIgnore(true) { extractIntegerInRange(1, secondNumMax, true) }
                if (secondNum == null) {
                    return if (preferMonthBeforeDay && firstNum <= 12) {
                        result.withDayOfMonth(1).withMonth(firstNum)
                    } else {
                        result.withDayOfMonth(firstNum)
                    }
                } else {
                    // Spanish standard is day-first (DD/MM), but we respect preferMonthBeforeDay
                    if ((preferMonthBeforeDay || secondNum > 12) && firstNum <= 12) {
                        result.withDayOfMonth(secondNum).withMonth(firstNum)
                    } else {
                        result.withDayOfMonth(firstNum).withMonth(secondNum)
                    }
                }
            }
        } else {
            // Date format includes a month name, e.g., "diciembre 25"
            result = result.withMonth(monthName)
            val dayNum = firstNum ?: ts.tryOrSkipDateTimeIgnore(true) { extractIntegerInRange(1, 31, true) }
            result = dayNum?.let { result.withDayOfMonth(it) } ?: result.withDayOfMonth(1)
        }
        val dayOrMonthFound = firstNum != null || monthName != null

        var bcad = ts.tryOrSkipDateTimeIgnore(dayOrMonthFound) { dateTimeExtractor.bcad() }
        val year = ts.tryOrSkipDateTimeIgnore(dayOrMonthFound && bcad == null) { extractIntegerInRange(0, 999999999) }

        if (year == null) {
            return if (dayOrMonthFound) result else null
        }

        if (bcad == null) {
            bcad = dateTimeExtractor.bcad()
        }
        // Spanish linguistics: "a.C." (antes de Cristo) means Before Christ.
        return result.withYear(year * (if (bcad == null || bcad) 1 else -1))
    }

    fun dayOfWeek(): Int? {
        // Spanish context: "mar" is ambiguous for "martes" (Tuesday) and "marzo" (March).
        // This logic is correct and mirrors the Italian version's ambiguity.
        if (ts[0].isValue("mar")) {
            ts.movePositionForwardBy(1)
            return 1 // Tuesday
        } else {
            return dateTimeExtractor.dayOfWeek()
        }
    }

    fun specialMinute(): Int? {
        // Spanish context: handles "y cuarto" (15), "y media" (30), "menos cuarto" (-15).
        val originalPosition = ts.position

        val isMinus = ts[0].isValue("menos")
        val isPlus = ts[0].isValue("y") || ts[0].isValue("con")

        // Look for 'cuarto' or 'media'
        val keywordIndex = if (isMinus || isPlus) 1 else 0
        if (ts[keywordIndex].isValue("cuarto")) {
            ts.movePositionForwardBy(keywordIndex + 1)
            return if (isMinus) -15 else 15
        }
        if (ts[keywordIndex].isValue("media")) {
            ts.movePositionForwardBy(keywordIndex + 1)
            return if (isMinus) -30 else 30 // "menos media" is unusual for non-native speakers but possible
        }
        
        ts.position = originalPosition
        return null
    }

    fun noonMidnightLike(): Int? = noonMidnightLikeOrMomentOfDay("noon_midnight_like")

    fun momentOfDay(): Int? = noonMidnightLikeOrMomentOfDay("moment_of_day")

    private fun noonMidnightLikeOrMomentOfDay(category: String): Int? {
        val originalPosition = ts.position
        var relativeIndicator = 0 // 0 = not found, otherwise the sign, +1 or -1
        if (ts[0].hasCategory("pre_special_hour")) {
            if (ts[0].hasCategory("pre_relative_indicator")) {
                relativeIndicator = if (ts[0].hasCategory("negative")) -1 else 1
                ts.movePositionForwardBy(ts.indexOfWithoutCategory("date_time_ignore", 1))
            } else {
                ts.movePositionForwardBy(1)
            }
        }

        if (ts[0].hasCategory(category)) {
            // e.g. mediodía, tarde, noche
            ts.movePositionForwardBy(1)
            return (ts[-1].number!!.integerValue().toInt() + DateTimeExtractorUtils.HOURS_IN_DAY + relativeIndicator) % DateTimeExtractorUtils.HOURS_IN_DAY
        }

        ts.position = originalPosition
        return null
    }

    fun hour(): Int? {
        val originalPosition = ts.position
        // skip words that usually come before hours, e.g. "a las", "hora"
        ts.movePositionForwardBy(ts.indexOfWithoutCategory("pre_hour", 0))

        val number = extractIntegerInRange(0, DateTimeExtractorUtils.HOURS_IN_DAY)
        if (number == null) {
            ts.position = originalPosition
            return null
        }
        return number % DateTimeExtractorUtils.HOURS_IN_DAY // transform 24 into 0
    }

    private fun relativeSpecialDay(): LocalDate? {
        val days = Utils.firstNotNull(
            this::relativeYesterday,
            dateTimeExtractor::relativeToday,
            this::relativeTomorrow,
            dateTimeExtractor::relativeDayOfWeekDuration
        )
        return days?.let { now.toLocalDate().plusDays(it.toLong()) }
    }

    fun relativeYesterday(): Int? {
        // Spanish context: "anteayer" is a single word for "day before yesterday".
        // The complex multi-word logic from English/Italian is not needed.
        if (ts[0].hasCategory("day_before_yesterday")) {
            ts.movePositionForwardBy(1)
            return -2
        }
        if (ts[0].hasCategory("yesterday")) {
            ts.movePositionForwardBy(1)
            return -1
        }
        return null
    }

    fun relativeTomorrow(): Int? {
        // Spanish context: "pasado mañana" is a single token for "day after tomorrow".
        if (ts[0].hasCategory("day_after_tomorrow")) {
            ts.movePositionForwardBy(1)
            return 2
        }
        if (ts[0].hasCategory("tomorrow")) {
            ts.movePositionForwardBy(1)
            return 1
        }
        return null
    }

    fun relativeDuration(): Duration? {
        // Spanish context: Handles "hace [duration]" (ago) and "[duration] después" (later).
        return dateTimeExtractor.relativeIndicatorDuration(
            durationExtractor::duration,
            { duration -> duration.multiply(-1) }
        )
    }
}