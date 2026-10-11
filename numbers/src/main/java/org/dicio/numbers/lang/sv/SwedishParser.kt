package org.dicio.numbers.lang.sv

import org.dicio.numbers.parser.Parser
import org.dicio.numbers.parser.lexer.TokenStream
import org.dicio.numbers.unit.Duration
import org.dicio.numbers.unit.Number
import org.dicio.numbers.util.DurationExtractorUtils
import java.time.LocalDateTime

class SwedishParser : Parser("config/sv-se") {
    override fun extractNumber(
        tokenStream: TokenStream,
        shortScale: Boolean,
        preferOrdinal: Boolean,
        integerOnly: Boolean
    ): () -> Number? {
        val numberExtractor = SwedishNumberExtractor(tokenStream, shortScale)
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
        val numberExtractor = SwedishNumberExtractor(tokenStream, shortScale)
        return DurationExtractorUtils(tokenStream, numberExtractor::numberNoOrdinal)::duration
    }

    override fun extractDateTime(
        tokenStream: TokenStream,
        shortScale: Boolean,
        preferMonthBeforeDay: Boolean,
        now: LocalDateTime
    ): () -> LocalDateTime? {
        return SwedishDateTimeExtractor(tokenStream, shortScale, preferMonthBeforeDay, now)::dateTime
    }
}
