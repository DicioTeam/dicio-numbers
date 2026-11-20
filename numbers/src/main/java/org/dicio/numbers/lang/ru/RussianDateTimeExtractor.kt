package org.dicio.numbers.lang.ru

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

/**
 * Based on ItalianDateTimeExtractor, with these differences:
 * - days of month are ordinal numbers, e.g. "пятого мая";
 * - "час"/"часов" and "минут" come after the numbers, e.g. "в семь часов тридцать минут";
 * - the moment of day comes after the time, e.g. "в три часа дня";
 * - позавчера and послезавтра are single words;
 * - years can be followed by "года", e.g. "пятого мая 2027 года".
 */
class RussianDateTimeExtractor internal constructor(
    private val ts: TokenStream,
    private val now: LocalDateTime
) {
    private val numberExtractor = RussianNumberExtractor(ts)
    private val durationExtractor = DurationExtractorUtils(ts, numberExtractor::numberNoOrdinal)
    private val dateTimeExtractor = DateTimeExtractorUtils(ts, now, this::extractIntegerInRange)

    private fun extractIntegerInRange(fromInclusive: Int, toInclusive: Int): Int? {
        // disallow fraction as / should be treated as a day/month/year separator
        return NumberExtractorUtils.extractOneIntegerInRange(
            ts, fromInclusive, toInclusive
        ) { NumberExtractorUtils.signBeforeNumber(ts) { numberExtractor.numberInteger(false) } }
    }

    private fun extractDayOfMonth(): Int? {
        // both "пятого мая" and "5 мая"
        return NumberExtractorUtils.extractOneIntegerInRange(ts, 1, 31) {
            numberExtractor.numberInteger(true)
        }
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
            date = ts.tryOrSkipDateTimeIgnore(true) { relativeSpecialDay() }

            if (date == null) {
                // then try with duration, since otherwise numbers would be interpreted as date days
                val duration = ts.tryOrSkipDateTimeIgnore(true) {
                    Utils.firstNotNull(this::relativeDuration, dateTimeExtractor::relativeMonthDuration)
                }
                if (duration == null) {
                    // no normal relative duration found: finally try extracting a date normally
                    date = ts.tryOrSkipDateTimeIgnore(true) { date() }
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
            time = ts.tryOrSkipDateTimeIgnore(date != null || timeFirst) { this.timeWithAmpm() }
        }

        if (date == null && time != null) {
            // try to extract a date after the time
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
            // if there is no time, maybe there is a moment of day preceding? e.g. утром в семь
            val momentOfDay = momentOfDay() ?: return null

            time = ts.tryOrSkipDateTimeIgnore(true) { this.time() }
            if (time == null) {
                // found moment of day without a specific time, e.g. вечером
                return LocalTime.of(momentOfDay, 0)
            } else {
                pm = DateTimeExtractorUtils.isMomentOfDayPm(momentOfDay)
            }
        } else {
            // found a time, now look for a moment of day, e.g. в три часа дня
            pm = ts.tryOrSkipDateTimeIgnore(true) {
                momentOfDay()?.let(DateTimeExtractorUtils::isMomentOfDayPm)
            }
        }

        if (time.hour != 0 && pm != null) {
            if (pm && !DateTimeExtractorUtils.isMomentOfDayPm(time.hour)) {
                // time must be in the afternoon, but time is not already, correct it
                time = time.withHour((time.hour + 12) % DateTimeExtractorUtils.HOURS_IN_DAY)
            } else if (!pm && time.hour == 12) {
                time = time.withHour(0) // e.g. двенадцать ночи
            }
        }
        return time
    }

    fun time(): LocalTime? {
        // try both with a normal hour and with "полдень"/"полночь"
        val hour = Utils.firstNotNull(this::noonMidnightLike, this::hour) ?: return null
        var result = LocalTime.of(hour, 0)

        if (ts[0].hasCategory("oclock_combined")) {
            ts.movePositionForwardBy(1) // e.g. в семь часов ровно
            return result
        }

        val minute = ts.tryOrSkipDateTimeIgnore(true) {
            Utils.firstNotNull(this::specialMinute, dateTimeExtractor::minute)
        } ?: return result
        result = result.withMinute(minute)

        val second = ts.tryOrSkipDateTimeIgnore(true) { dateTimeExtractor.second() }
            ?: return result
        return result.withSecond(second)
    }

    fun date(): LocalDate? {
        var result = now.toLocalDate()

        val dayOfWeek = dateTimeExtractor.dayOfWeek()
        val day = ts.tryOrSkipDateTimeIgnore(dayOfWeek != null) { extractDayOfMonth() }

        // below here, we do result.withMonth(1) because January has 31 days, so setting the
        // withDayOfMonth() below will always succeed, and then we overwrite/reset the month anyway
        val originalMonth = result.month.value
        if (day == null) {
            if (dayOfWeek != null) {
                return result.plus((dayOfWeek - result.dayOfWeek.ordinal).toLong(), ChronoUnit.DAYS)
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
                // e.g. "пятнадцатого", "в пятницу пятнадцатого": current month
                return result.withMonth(originalMonth)
            }
            return null
        } else {
            result = result.withMonth(month)
        }

        val year = ts.tryOrSkipDateTimeIgnore(true) {
            NumberExtractorUtils.extractOneIntegerInRange(ts, 0, 999999999) {
                numberExtractor.numberInteger(true) // e.g. 2027, две тысячи двадцать седьмого
            }
        } ?: return result

        if (ts[0].asDurationToken?.durationCategory == "1 YEARS") {
            ts.movePositionForwardBy(1) // e.g. 2027 года
        }
        val bcad = dateTimeExtractor.bcad()
        return result.withYear(year * (if (bcad == null || bcad) 1 else -1))
    }

    fun specialMinute(): Int? {
        val originalPosition = ts.position

        val number = numberExtractor.numberNoOrdinal()
        if (number != null && number.isDecimal && number.decimalValue() > 0.0 && number.decimalValue() < 1.0) {
            // e.g. в два и три четверти
            return Utils.roundToInt(number.decimalValue() * 60)
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
            ts.movePositionForwardBy(1) // e.g. этим вечером
        }

        if (ts[0].hasCategory(category)) {
            // special hour found, e.g. полночь, вечером
            ts.movePositionForwardBy(1)
            return ts[-1].number!!.integerValue().toInt()
        }

        ts.position = originalPosition
        return null
    }

    fun hour(): Int? {
        val originalPosition = ts.position

        // skip words that usually come before hours, e.g. в, к, около
        ts.movePositionForwardBy(ts.indexOfWithoutCategory("pre_hour", 0))

        val number = extractIntegerInRange(0, DateTimeExtractorUtils.HOURS_IN_DAY)
        if (number == null) {
            // no number found, or the number is not a valid hour, e.g. в двадцать шесть
            ts.position = originalPosition
            return null
        }

        if (ts[0].asDurationToken?.durationCategory == "1 HOURS") {
            ts.movePositionForwardBy(1) // e.g. в семь часов
        }
        return number % DateTimeExtractorUtils.HOURS_IN_DAY // transform 24 into 0
    }


    private fun relativeSpecialDay(): LocalDate? {
        val days = Utils.firstNotNull(
            this::relativeYesterdayOrTomorrow,
            dateTimeExtractor::relativeToday,
            dateTimeExtractor::relativeDayOfWeekDuration
        ) ?: return null
        return now.toLocalDate().plusDays(days.toLong())
    }

    private fun relativeYesterdayOrTomorrow(): Int? {
        val days = when {
            ts[0].hasCategory("before_yesterday") -> -2
            ts[0].hasCategory("yesterday") -> -1
            ts[0].hasCategory("tomorrow") -> 1
            ts[0].hasCategory("after_tomorrow") -> 2
            else -> return null
        }
        ts.movePositionForwardBy(1)
        return days
    }

    fun relativeDuration(): Duration? {
        return dateTimeExtractor.relativeIndicatorDuration(
            { durationExtractor.duration() },
            { duration -> duration.multiply(-1) }
        )
    }
}
