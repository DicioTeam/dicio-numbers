package org.dicio.numbers.parser.param

import org.dicio.numbers.parser.Parser
import org.dicio.numbers.parser.lexer.TokenStream
import java.time.LocalDateTime

class ExtractDateTimeParams(parser: Parser, utterance: String) :
    ParserParams<LocalDateTime>(parser, utterance) {
    // default values
    private var now: LocalDateTime = LocalDateTime.now()
    private var shortScale = true // shouldn't make a big difference anyway
    private var preferMonthBeforeDay = false

    /**
     * @param now the date/time to use as a base for relative date/times and for when some fields
     * are missing in the string being parsed. **The default is [LocalDateTime.now]`()`.**
     * @return this
     */
    fun now(now: LocalDateTime): ExtractDateTimeParams {
        this.now = now
        return this
    }

    /**
     * @param shortScale consider large numbers that make up date/times (e.g. the "one billion" in
     * "one billion milliseconds ago") as short scale (true) or long scale (false) numbers (see
     * [Names of large numbers](https://en.wikipedia.org/wiki/Names_of_large_numbers)). **The
     * default is `true`.** This setting only affects languages where both short and long scale
     * numbers exist, e.g. English, and is otherwise ignored.
     * @return this
     */
    fun shortScale(shortScale: Boolean): ExtractDateTimeParams {
        this.shortScale = shortScale
        return this
    }

    /**
     * @param preferMonthBeforeDay whether in case of ambiguities the month should be considered as
     * coming before the day. E.g. "1.2.2024" will be interpreted as "1st of February 2024" if this
     * is `false`, and as "2nd of January 2024" if this is `true`. **The default is `false`.**
     * @return this
     */
    fun preferMonthBeforeDay(preferMonthBeforeDay: Boolean): ExtractDateTimeParams {
        this.preferMonthBeforeDay = preferMonthBeforeDay
        return this
    }

    fun get(): LocalDateTime? {
        return parseFirst()
    }

    override fun getExtractorAtCurrentPosition(tokenStream: TokenStream): () -> LocalDateTime? {
        return parser.extractDateTime(tokenStream, shortScale, preferMonthBeforeDay, now)
    }
}
