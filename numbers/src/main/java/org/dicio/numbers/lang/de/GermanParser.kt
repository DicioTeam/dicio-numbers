package org.dicio.numbers.lang.de

import org.dicio.numbers.parser.Parser
import org.dicio.numbers.parser.lexer.TokenStream
import org.dicio.numbers.unit.Duration
import org.dicio.numbers.unit.Number
import org.dicio.numbers.util.DurationExtractorUtils
import java.time.LocalDateTime

class GermanParser : Parser("config/de-de") {
    override fun extractNumber(
        tokenStream: TokenStream,
        shortScale: Boolean,
        preferOrdinal: Boolean,
        integerOnly: Boolean
    ): () -> Number? {
        // German always uses the long scale (Million, Milliarde, Billion, ...), since the scale is
        // part of the words themselves; shortScale is therefore ignored
        val numberExtractor = GermanNumberExtractor(tokenStream)
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
        val numberExtractor = GermanNumberExtractor(tokenStream)
        return DurationExtractorUtils(tokenStream, numberExtractor::numberNoOrdinal)::duration
    }

    override fun extractDateTime(
        tokenStream: TokenStream,
        shortScale: Boolean,
        preferMonthBeforeDay: Boolean,
        now: LocalDateTime
    ): () -> LocalDateTime? {
        return GermanDateTimeExtractor(tokenStream, now)::dateTime
    }
}
