package org.dicio.numbers.lang.ru

import org.dicio.numbers.parser.Parser
import org.dicio.numbers.parser.lexer.TokenStream
import org.dicio.numbers.unit.Duration
import org.dicio.numbers.unit.Number
import org.dicio.numbers.util.DurationExtractorUtils
import java.time.LocalDateTime

class RussianParser : Parser("config/ru-ru") {
    override fun extractNumber(
        tokenStream: TokenStream,
        shortScale: Boolean,
        preferOrdinal: Boolean,
        integerOnly: Boolean
    ): () -> Number? {
        // Russian uses only the short scale (миллиард = 10^9), so shortScale is ignored
        val numberExtractor = RussianNumberExtractor(tokenStream)
        return when {
            integerOnly -> numberExtractor::numberMustBeInteger
            preferOrdinal -> numberExtractor::numberPreferOrdinal
            else -> numberExtractor::numberPreferFraction
        }
    }

    override fun extractDuration(
        tokenStream: TokenStream,
        shortScale: Boolean
    ): () -> Duration? {
        val numberExtractor = RussianNumberExtractor(tokenStream)
        return DurationExtractorUtils(tokenStream, numberExtractor::numberNoOrdinal)::duration
    }

    override fun extractDateTime(
        tokenStream: TokenStream,
        shortScale: Boolean,
        preferMonthBeforeDay: Boolean,
        now: LocalDateTime
    ): () -> LocalDateTime? {
        return RussianDateTimeExtractor(tokenStream, now)::dateTime
    }
}
