package org.dicio.numbers.lang.fr

import org.dicio.numbers.parser.Parser
import org.dicio.numbers.parser.lexer.TokenStream
import org.dicio.numbers.unit.Duration
import org.dicio.numbers.unit.Number
import org.dicio.numbers.util.DurationExtractorUtils
import java.time.LocalDateTime

class FrenchParser : Parser("config/fr-fr") {
    override fun extractNumber(
        tokenStream: TokenStream,
        shortScale: Boolean,
        preferOrdinal: Boolean,
        integerOnly: Boolean
    ): () -> Number? {
        val numberExtractor = FrenchNumberExtractor(tokenStream)
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
        val numberExtractor = FrenchNumberExtractor(tokenStream)
        return DurationExtractorUtils(tokenStream, numberExtractor::numberNoOrdinal)::duration
    }

    override fun extractDateTime(
        tokenStream: TokenStream,
        shortScale: Boolean,
        preferMonthBeforeDay: Boolean,
        now: LocalDateTime
    ): () -> LocalDateTime? {
        return FrenchDateTimeExtractor(tokenStream, now)::dateTime
    }
}
