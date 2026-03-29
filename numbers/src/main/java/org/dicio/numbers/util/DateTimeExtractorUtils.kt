package org.dicio.numbers.util

import org.dicio.numbers.parser.lexer.TokenStream
import org.dicio.numbers.unit.Duration
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.Month

/**
 * This class should work well at least for european languages (I don't know the structure of
 * other languages though). Requires the token stream to have been tokenized with the same rules
 * as in the English language.
 *
 * @param ts the token stream from which to obtain information
 */
class DateTimeExtractorUtils(
    private val ts: TokenStream,
    private val now: LocalDateTime,
    private val extractIntegerInRange: (Int, Int) -> Int?
) {
    fun ampm(): Boolean? {
        return bcadOrAmpm("ampm")
    }

    fun bcad(): Boolean? {
        return bcadOrAmpm("bcad")
    }

    /**
     * @param prefix either "bcad" or "ampm", i.e. the prefix to use for the following categories:
     * _before, _after, _identifier, _before_combined, _after_combined
     * @return false if before+identifier matches, true if after+identifier matches, null otherwise
     */
    private fun bcadOrAmpm(prefix: String): Boolean? {
        ts.movePositionForwardBy(1)
        val result = if (ts[-1].hasCategory(prefix + "_before")) {
            false
        } else if (ts[-1].hasCategory(prefix + "_after")) {
            true
        } else if (ts[-1].hasCategory(prefix + "_before_combined")) {
            // found am or bc in a single word -> return "before"
            return false
        } else if (ts[-1].hasCategory(prefix + "_after_combined")) {
            // found pm or ad in a single word -> return "after"
            return true
        } else {
            // nothing related to bc/ad/am/pm here (even if the identifier might match on its own)
            ts.movePositionForwardBy(-1)
            return null
        }

        // we can't use ts.indexOfWithoutCategory, since some ignore words might be identifiers
        val foundIdentifier = ts.tryOrSkipCategory(
            "ignore", true
        ) { if (ts[0].hasCategory(prefix + "_identifier")) true else null }
        if (foundIdentifier != null) {
            ts.movePositionForwardBy(1)
            return result
        } else {
            ts.movePositionForwardBy(-1)
            return null
        }
    }


    fun monthName(): Int? {
        if (ts[0].hasCategory("month_name")) {
            ts.movePositionForwardBy(1)
            return ts[-1].number!!.integerValue().toInt()
        } else {
            return null
        }
    }

    fun dayOfWeek(): Int? {
        if (ts[0].hasCategory("day_of_week")) {
            ts.movePositionForwardBy(1)
            return ts[-1].number!!.integerValue().toInt()
        } else {
            return null
        }
    }


    fun second(): Int? {
        return minuteOrSecond("1 SECONDS")
    }

    fun minute(): Int? {
        return minuteOrSecond("1 MINUTES")
    }

    fun minuteOrSecond(durationCategory: String): Int? {
        val number = extractIntegerInRange(0, 59) ?: return null

        if (ts[0].asDurationToken?.durationCategory == durationCategory) {
            // skip "minuti"/"secondi" said after a minute/second count, e.g. ventiquattro minuti
            ts.movePositionForwardBy(1)
        }

        return number
    }


    fun relativeToday(): Int? {
        if (ts[0].hasCategory("today")) {
            ts.movePositionForwardBy(1)
            return 0 // no offset
        } else {
            return null
        }
    }

    fun relativeYesterday(): Int? {
        if (ts[0].hasCategory("yesterday")) {
            ts.movePositionForwardBy(1)
            return -1
        } else {
            return null
        }
    }

    fun relativeTomorrow(): Int? {
        if (ts[0].hasCategory("tomorrow")) {
            ts.movePositionForwardBy(1)
            return 1
        } else {
            return null
        }
    }

    fun relativeDayOfWeekDuration(): Int? {
        return relativeIndicatorDuration({
            var number = extractIntegerInRange(1, Int.MAX_VALUE)
            if (number == null) {
                // there does not need to be a number, e.g. giovedì prossimo
                number = 1
            } else {
                // found a number, e.g. fra due
                ts.movePositionForwardBy(ts.indexOfWithoutCategory("date_time_ignore", 0))
            }
            if (ts[0].hasCategory("day_of_week")) {
                // found a day of week, e.g. giovedì
                val daysDifference = ts[0].number!!.integerValue().toInt() - now.dayOfWeek.ordinal
                val daysOffset =
                    ((daysDifference + DAYS_IN_WEEK) % DAYS_IN_WEEK // add a week if the two days coincide
                            + (if (daysDifference == 0) DAYS_IN_WEEK else 0) // sum some additional weeks if the input says so
                            + ((number - 1) * DAYS_IN_WEEK))
                ts.movePositionForwardBy(1)
                return@relativeIndicatorDuration daysOffset
            } else {
                return@relativeIndicatorDuration null
            }
        }, { daysOffset: Int ->
            // the congruency modulo DAYS_IN_WEEK is 0: just use a minus to maintain it
            if (daysOffset % DAYS_IN_WEEK == 0) -daysOffset
            // keep congruency modulo DAYS_IN_WEEK, taking care of additional weeks
            else 2 * (daysOffset % DAYS_IN_WEEK) - DAYS_IN_WEEK - daysOffset
        })
    }

    fun relativeMonthDuration(): Duration? {
        val months = relativeIndicatorDuration({
            if (ts[0].hasCategory("month_name")) {
                val monthsDifference = ts[0].number!!.integerValue() - now.month.value
                val monthsOffset =
                    ((monthsDifference + MONTHS_IN_YEAR) % MONTHS_IN_YEAR // add a year if the two months coincide
                            + (if (monthsDifference == 0L) MONTHS_IN_YEAR else 0))
                ts.movePositionForwardBy(1)
                return@relativeIndicatorDuration monthsOffset
            }
            null
        }, { monthsOffset: Long ->
            // the congruency modulo MONTHS_IN_YEAR is 0: just use a minus to maintain it
            if (monthsOffset == MONTHS_IN_YEAR) -MONTHS_IN_YEAR
            // keep congruency modulo MONTHS_IN_YEAR
            else monthsOffset - MONTHS_IN_YEAR
        })

        return if (months == null) null else Duration(0, 0, months, 0)
    }

    fun <T> relativeIndicatorDuration(
        durationExtractor: () -> T?,
        oppositeDuration: (T) -> T
    ): T? {
        val originalTsPosition = ts.position

        var relativeIndicator = 0 // 0 = not found, otherwise the sign, +1 or -1
        if (ts[0].hasCategory("pre_relative_indicator")) {
            // there is a relative indicator before, e.g. fra
            relativeIndicator = if (ts[0].hasCategory("negative")) -1 else 1
            ts.movePositionForwardBy(ts.indexOfWithoutCategory("date_time_ignore", 1))
        }

        val result: T? = durationExtractor()
        if (result == null) {
            // no duration found, e.g. tra sei ciao
            ts.position = originalTsPosition
            return null
        }

        val nextNotIgnore = ts.indexOfWithoutCategory("date_time_ignore", 0)
        if (relativeIndicator == 0
            && ts[nextNotIgnore].hasCategory("post_relative_indicator")
        ) {
            // there is a relative indicator after, e.g. due settimane fa
            relativeIndicator = if (ts[nextNotIgnore].hasCategory("negative")) -1 else 1
            ts.movePositionForwardBy(nextNotIgnore + 1)
        }

        if (relativeIndicator == 0) {
            // no relative indicator found, this is not a relative duration, e.g. sei mesi
            ts.position = originalTsPosition
            return null
        } else {
            // found relative duration, e.g. tra due minuti
            return if (relativeIndicator == -1) oppositeDuration(result) else result
        }
    }

    companion object {
        const val HOURS_IN_DAY: Int = 24
        val DAYS_IN_WEEK: Int = DayOfWeek.entries.size // 7
        val MONTHS_IN_YEAR: Long = Month.entries.size.toLong() // 12

        @JvmStatic
        fun isMomentOfDayPm(momentOfDay: Int): Boolean {
            return momentOfDay >= 12
        }
    }
}
