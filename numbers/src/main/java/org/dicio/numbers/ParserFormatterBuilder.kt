package org.dicio.numbers

import org.dicio.numbers.formatter.Formatter
import org.dicio.numbers.lang.en.EnglishFormatter
import org.dicio.numbers.lang.en.EnglishParser
import org.dicio.numbers.lang.es.SpanishFormatter
import org.dicio.numbers.lang.es.SpanishParser
import org.dicio.numbers.lang.it.ItalianFormatter
import org.dicio.numbers.lang.it.ItalianParser
import org.dicio.numbers.parser.Parser
import java.util.Locale

object ParserFormatterBuilder {
    private val PARSER_FORMATTER_CLASSES_MAP = mapOf(
        "en" to ParserFormatterClasses(EnglishFormatter::class.java, EnglishParser::class.java),
        "it" to ParserFormatterClasses(ItalianFormatter::class.java, ItalianParser::class.java),
        "es" to ParserFormatterClasses(SpanishFormatter::class.java, SpanishParser::class.java),
    )

    @JvmStatic
    @Throws(IllegalArgumentException::class)
    fun parserFormatterPairForLocale(locale: Locale): ParserFormatterPair {
        val localeString = resolveLocaleString(locale, PARSER_FORMATTER_CLASSES_MAP.keys)
        val classes = PARSER_FORMATTER_CLASSES_MAP[localeString]!!

        try {
            return ParserFormatterPair(
                classes.formatter.getDeclaredConstructor().newInstance(),
                classes.parser.getDeclaredConstructor().newInstance()
            )
        } catch (e: InstantiationException) {
            throw IllegalArgumentException(
                "The formatter and parser for the provided supported"
                        + " locale \"" + locale + "\" could not be instantiated", e
            )
        } catch (e: IllegalAccessException) {
            throw IllegalArgumentException(
                "The formatter and parser for the provided supported"
                        + " locale \"" + locale + "\" could not be instantiated", e
            )
        }
    }


    // copied from dicio-android
    @Throws(IllegalArgumentException::class)
    private fun resolveLocaleString(
        locale: Locale,
        supportedLocales: Collection<String>
    ): String {
        // first try with full locale name (e.g. en-US)
        var localeString = (locale.language + "-" + locale.country).lowercase(Locale.getDefault())
        if (supportedLocales.contains(localeString)) {
            return localeString
        }

        // then try with only base language (e.g. en)
        localeString = locale.language.lowercase(Locale.getDefault())
        if (supportedLocales.contains(localeString)) {
            return localeString
        }

        // then try with children languages of locale base language (e.g. en-US, en-GB, en-UK, ...)
        for (supportedLocalePlus in supportedLocales) {
            for (supportedLocale in supportedLocalePlus.split("\\+".toRegex())
                .dropLastWhile { it.isEmpty() }
                .toTypedArray()) {
                if (supportedLocale.split("-".toRegex(), limit = 2)
                        .toTypedArray()[0] == localeString
                ) {
                    return supportedLocalePlus
                }
            }
        }

        // fail
        throw IllegalArgumentException("Unsupported locale: $locale")
    }

    private class ParserFormatterClasses(
        val formatter: Class<out Formatter>,
        val parser: Class<out Parser>,
    )

    class ParserFormatterPair(
        @JvmField val formatter: Formatter,
        @JvmField val parser: Parser,
    )
}
