package org.dicio.numbers.parser.param

import org.dicio.numbers.parser.Parser
import org.dicio.numbers.parser.lexer.TokenStream
import org.dicio.numbers.unit.Duration

class ExtractDurationParams(parser: Parser, utterance: String) :
    ParserParams<Duration>(parser, utterance) {
    // default values
    private var shortScale = true

    /**
     * @param shortScale consider large numbers that make up durations (e.g. the "one billion" in
     * "one billion milliseconds") as short scale (true) or long scale (false) numbers (see
     * [Names of large numbers](https://en.wikipedia.org/wiki/Names_of_large_numbers)). **The
     * default is `true`.** This setting only affects languages where both short and long scale
     * numbers exist, e.g. English, and is otherwise ignored.
     * @return this
     */
    fun shortScale(shortScale: Boolean): ExtractDurationParams {
        this.shortScale = shortScale
        return this
    }

    fun get(): Duration? {
        return parseFirst()
    }

    override fun getExtractorAtCurrentPosition(tokenStream: TokenStream): () -> Duration? {
        return parser.extractDuration(tokenStream, shortScale)
    }
}
