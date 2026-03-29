package org.dicio.numbers.lang.fr

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

class FrenchDateTimeExtractor internal constructor(
    private val ts: TokenStream,
    private val now: LocalDateTime
) {
    private val numberExtractor = FrenchNumberExtractor(ts)
    private val durationExtractor = DurationExtractorUtils(ts, numberExtractor::numberNoOrdinal)
    private val dateTimeExtractor = DateTimeExtractorUtils(ts, now, this::extractIntegerInRange)

    private fun extractIntegerInRange(fromInclusive: Int, toInclusive: Int): Int? {
        return NumberExtractorUtils.extractOneIntegerInRange(
            ts, fromInclusive, toInclusive
        ) { NumberExtractorUtils.signBeforeNumber(ts) { numberExtractor.numberInteger(false) } }
    }

    fun dateTime(): LocalDateTime? {
        return ts.firstWhichUsesMostTokens({ dateTime(false) }, { dateTime(true) })
    }

    private fun dateTime(timeFirst: Boolean): LocalDateTime? {
        var date: LocalDate? = null
        var time: LocalTime? = null

        if (!timeFirst) {
            date = relativeSpecialDay()

            if (date == null) {
                val duration = Utils.firstNotNull(
                    this::relativeDuration,
                    dateTimeExtractor::relativeMonthDuration
                )
                if (duration == null) {
                    date = date()
                } else if (duration.nanos == 0L && duration.days != 0L) {
                    date = duration.applyAsOffsetToDateTime(now).toLocalDate()
                } else if (duration.nanos != 0L && duration.days == 0L
                    && duration.months == 0L && duration.years == 0L
                ) {
                    time = duration.applyAsOffsetToDateTime(now).toLocalTime()
                } else {
                    return duration.applyAsOffsetToDateTime(now)
                }
            }
        }

        if (time == null) {
            time = ts.tryOrSkipDateTimeIgnore(date != null) { this.timeWithAmpm() }
        }

        if (date == null && time != null) {
            val originalPosition = ts.position
            val duration = ts.tryOrSkipDateTimeIgnore(true) { this.relativeDuration() }
            if (duration == null) {
                date = ts.tryOrSkipDateTimeIgnore(true) {
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

    fun timeWithAmpm(): LocalTime? {
        var time = time()
        val pm: Boolean?
        if (time == null) {
            val momentOfDay = momentOfDay() ?: return null
            time = ts.tryOrSkipDateTimeIgnore(true) { this.time() }
            if (time == null) {
                return LocalTime.of(momentOfDay, 0)
            } else {
                pm = DateTimeExtractorUtils.isMomentOfDayPm(momentOfDay)
            }
        } else {
            pm = ts.tryOrSkipDateTimeIgnore(true) {
                Utils.firstNotNull(
                    dateTimeExtractor::ampm,
                    { momentOfDay()?.let(DateTimeExtractorUtils::isMomentOfDayPm) }
                )
            }
        }

        if (time.hour != 0 && pm != null) {
            if (pm && !DateTimeExtractorUtils.isMomentOfDayPm(time.hour)) {
                time = time.withHour((time.hour + 12) % DateTimeExtractorUtils.HOURS_IN_DAY)
            }
        }
        return time
    }

    fun time(): LocalTime? {
        val hour = Utils.firstNotNull(this::noonMidnightLike, this::hour) ?: return null
        var result = LocalTime.of(hour, 0)

        val minute = ts.tryOrSkipDateTimeIgnore(true) {
            Utils.firstNotNull(this::specialMinute, dateTimeExtractor::minute)
        }
        if (minute == null) return result
        result = result.withMinute(minute)

        val second = ts.tryOrSkipDateTimeIgnore(true) { dateTimeExtractor.second() }
        if (second == null) return result
        return result.withSecond(second)
    }

    fun date(): LocalDate? {
        var result = now.toLocalDate()

        val dayOfWeek = dateTimeExtractor.dayOfWeek()
        val day = ts.tryOrSkipDateTimeIgnore(dayOfWeek != null) { extractIntegerInRange(1, 31) }

        val originalMonth = result.month.value
        if (day == null) {
            if (dayOfWeek != null) {
                return result.plus(
                    (dayOfWeek - result.dayOfWeek.ordinal).toLong(), ChronoUnit.DAYS
                )
            }
            result = result.withMonth(1).withDayOfMonth(1)
        } else {
            result = result.withMonth(1).withDayOfMonth(day)
        }

        val month = ts.tryOrSkipDateTimeIgnore(day != null) {
            Utils.firstNotNull(dateTimeExtractor::monthName, { extractIntegerInRange(1, 12) })
        }
        if (month == null) {
            if (day != null) {
                result = result.withMonth(originalMonth)
                return result
            }
        } else {
            result = result.withMonth(month)
        }

        val year = ts.tryOrSkipDateTimeIgnore(month != null) {
            extractIntegerInRange(0, 999999999)
        }
        if (year == null) {
            if (month != null) return result
            return null
        }

        val bcad = dateTimeExtractor.bcad()
        return result.withYear(year * (if (bcad == null || bcad) 1 else -1))
    }

    fun specialMinute(): Int? {
        val originalPosition = ts.position
        // Check for "moins le quart" = -15 minutes (relative to next hour)
        if (ts[0].hasCategory("special_minute_before")) {
            ts.movePositionForwardBy(ts.indexOfWithoutCategory("date_time_ignore", 1))
            val minute = extractIntegerInRange(1, 59)
            if (minute != null) {
                return 60 - minute
            }
        }
        ts.position = originalPosition
        return null
    }

    fun noonMidnightLike(): Int? {
        return noonMidnightLikeOrMomentOfDay("noon_midnight_like")
    }

    fun momentOfDay(): Int? {
        return noonMidnightLikeOrMomentOfDay("moment_of_day")
    }

    private fun noonMidnightLikeOrMomentOfDay(category: String): Int? {
        val originalPosition = ts.position

        var relativeIndicator = 0
        if (ts[0].hasCategory("pre_special_hour")) {
            if (ts[0].hasCategory("pre_relative_indicator")) {
                relativeIndicator = if (ts[0].hasCategory("negative")) -1 else 1
                ts.movePositionForwardBy(ts.indexOfWithoutCategory("date_time_ignore", 1))
            } else {
                ts.movePositionForwardBy(1)
            }
        }

        if (ts[0].hasCategory(category)) {
            ts.movePositionForwardBy(1)
            return ((ts[-1].number!!.integerValue().toInt()
                    + DateTimeExtractorUtils.HOURS_IN_DAY + relativeIndicator)
                    % DateTimeExtractorUtils.HOURS_IN_DAY)
        }

        ts.position = originalPosition
        return null
    }

    fun hour(): Int? {
        val originalPosition = ts.position
        ts.movePositionForwardBy(ts.indexOfWithoutCategory("pre_hour", 0))

        val number = extractIntegerInRange(0, DateTimeExtractorUtils.HOURS_IN_DAY)
        if (number == null) {
            ts.position = originalPosition
            return null
        }
        return number % DateTimeExtractorUtils.HOURS_IN_DAY
    }

    private fun relativeSpecialDay(): LocalDate? {
        val days = Utils.firstNotNull(
            dateTimeExtractor::relativeYesterday,
            dateTimeExtractor::relativeToday,
            dateTimeExtractor::relativeTomorrow,
            dateTimeExtractor::relativeDayOfWeekDuration
        )
        if (days == null) return null
        return now.toLocalDate().plusDays(days.toLong())
    }

    fun relativeDuration(): Duration? {
        return dateTimeExtractor.relativeIndicatorDuration(
            { durationExtractor.duration() },
            { duration -> duration.multiply(-1) }
        )
    }
}
