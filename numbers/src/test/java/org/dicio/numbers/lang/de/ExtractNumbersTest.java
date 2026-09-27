package org.dicio.numbers.lang.de;

import static org.dicio.numbers.test.TestUtils.F;
import static org.dicio.numbers.test.TestUtils.T;
import static org.dicio.numbers.test.TestUtils.n;
import static org.dicio.numbers.test.TestUtils.numberDeduceType;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.dicio.numbers.ParserFormatter;
import org.dicio.numbers.parser.lexer.TokenStream;
import org.dicio.numbers.test.WithTokenizerTestBase;
import org.dicio.numbers.unit.Number;
import org.junit.Test;

import java.util.function.Function;

public class ExtractNumbersTest extends WithTokenizerTestBase {

    @Override
    public String configFolder() {
        return "config/de-de";
    }


    private void assertNumberFunction(final String s,
                                      final Number value,
                                      final int finalTokenStreamPosition,
                                      final Function<GermanNumberExtractor, Number> numberFunction) {
        final TokenStream ts = new TokenStream(tokenizer.tokenize(s));
        final Number number = numberFunction.apply(new GermanNumberExtractor(ts));
        assertEquals("wrong value for string " + s, value, number);
        assertEquals("wrong final token position for number " + value, finalTokenStreamPosition,
                ts.position);
    }

    private void assertNumberFunctionNull(final String s,
                                          final Function<GermanNumberExtractor, Number> numberFunction) {
        assertNumberFunction(s, null, 0, numberFunction);
    }

    private void assertNumberInteger(final String s, final boolean allowOrdinal, final double value,
                                     final boolean isOrdinal, final int finalTokenStreamPosition) {
        assertNumberFunction(s, numberDeduceType(value).withOrdinal(isOrdinal),
                finalTokenStreamPosition, (gne) -> gne.numberInteger(allowOrdinal));
    }

    private void assertNumberIntegerNull(final String s, final boolean allowOrdinal) {
        assertNumberFunctionNull(s, (gne) -> gne.numberInteger(allowOrdinal));
    }

    private void assertNumberPoint(final String s, final double value,
                                   final int finalTokenStreamPosition) {
        assertNumberFunction(s, numberDeduceType(value), finalTokenStreamPosition,
                (gne) -> gne.numberPoint(false));
    }

    private void assertNumberPreferFraction(final String s, final double value,
                                            final int finalTokenStreamPosition) {
        assertNumberFunction(s, numberDeduceType(value), finalTokenStreamPosition,
                GermanNumberExtractor::numberPreferFraction);
    }


    @Test
    public void testNumberInteger() {
        // the tokenizer splits compound words, so the positions count the pieces
        assertNumberInteger("dreiundzwanzig",                        F, 23,         F, 3);
        assertNumberInteger("drei und zwanzig Äpfel",                F, 23,         F, 3);
        assertNumberInteger("zweihundertdreiundvierzig",             F, 243,        F, 5);
        assertNumberInteger("hunderteins",                           F, 101,        F, 2);
        assertNumberInteger("hundertundeins",                        F, 101,        F, 3);
        assertNumberInteger("einhundert",                            F, 100,        F, 2);
        assertNumberInteger("tausend",                               F, 1000,       F, 1);
        assertNumberInteger("eintausendeins",                        F, 1001,       F, 3);
        assertNumberInteger("zweitausenddreihundertvierundfünfzig",  F, 2354,       F, 7);
        assertNumberInteger("neunzehnhundertvierundachtzig",         F, 1984,       F, 5);
        assertNumberInteger("eine Million zweihunderttausend",       F, 1200000,    F, 5);
        assertNumberInteger("drei Milliarden vierhundert Millionen", F, 3.4e9,      F, 5);
        assertNumberInteger("siebzehn",                              F, 17,         F, 1);
        assertNumberInteger("achtzehnhundert",                       F, 1800,       F, 2);
        assertNumberInteger("zwo",                                   F, 2,          F, 1);
        assertNumberInteger("null",                                  F, 0,          F, 1);
        assertNumberInteger("eine Stunde",                           F, 1,          F, 1);
        assertNumberInteger("250 Gramm",                             F, 250,        F, 1);
        assertNumberInteger("1.000.000",                             F, 1000000,    F, 5);
        assertNumberInteger("zwei und drei",                         F, 2,          F, 1);
        assertNumberInteger("drei null",                             F, 3,          F, 1);
    }

    @Test
    public void testNumberIntegerOrdinal() {
        assertNumberInteger("dritte",             T, 3,       T, 1);
        assertNumberInteger("einundzwanzigsten",  T, 21,      T, 3);
        assertNumberInteger("hundertste",         T, 100,     T, 1);
        assertNumberInteger("zweihundertste",     T, 200,     T, 2);
        assertNumberInteger("hundertzweite",      T, 102,     T, 2);
        assertNumberInteger("tausendste",         T, 1000,    T, 1);
        assertNumberInteger("zweitausendste",     T, 2000,    T, 2);
        assertNumberInteger("dreizehnter",        T, 13,      T, 1);
        assertNumberInteger("3te",                T, 3,       T, 2);
        assertNumberIntegerNull("dritte",            F);
        assertNumberIntegerNull("einundzwanzigsten", F);
        assertNumberIntegerNull("3te",               F);
    }

    @Test
    public void testNumberIntegerNull() {
        assertNumberIntegerNull("",             F);
        assertNumberIntegerNull("hallo",        T);
        assertNumberIntegerNull("und zwanzig",  F);
        assertNumberIntegerNull("Viertel",      F);
        assertNumberIntegerNull("komma drei",   F);
    }

    @Test
    public void testNumberPoint() {
        assertNumberPoint("drei komma fünf",        3.5,  3);
        assertNumberPoint("3,5",                    3.5,  3);
        assertNumberPoint("null komma null fünf",   0.05, 4);
        assertNumberPoint("zweieinhalb",            2.5,  2);
        assertNumberPoint("eineinhalb Stunden",     1.5,  2);
        assertNumberPoint("anderthalb",             1.5,  1);
        assertNumberPoint("einhalb",                0.5,  1);
        assertNumberPoint("zehn geteilt durch vier", 2.5, 4);
        assertNumberPoint("zehn durch vier",        2.5,  3);
        assertNumberPoint("drei komma",             3,    1);
    }

    @Test
    public void testNumberPreferFraction() {
        assertNumberPreferFraction("drei viertel",      0.75,    2);
        assertNumberPreferFraction("dreiviertel",       0.75,    2);
        assertNumberPreferFraction("ein halb",          0.5,     2);
        assertNumberPreferFraction("eine halbe Stunde", 0.5,     2);
        assertNumberPreferFraction("zwei Drittel",      2.0 / 3, 2);
        assertNumberPreferFraction("sechzehn halbe",    8,       2);
        assertNumberPreferFraction("zwei Dutzend",      24,      2);
        assertNumberPreferFraction("fünf Prozent",      0.05,    2);
        assertNumberPreferFraction("halbe",             0.5,     1);
        assertNumberPreferFraction("minus sieben",      -7,      2);
    }

    @Test
    public void testNumberParserExtractNumber() {
        final ParserFormatter pf = new ParserFormatter(null, new GermanParser());
        assertNull(pf.extractNumber("hallo wie geht es dir").parseFirst());
        assertEquals(n(23), pf.extractNumber("ich habe dreiundzwanzig Äpfel").parseFirst());
        assertEquals(n(3, T), pf.extractNumber("der dritte Platz").preferOrdinal(T).parseFirst());
        assertEquals(n(0.25), pf.extractNumber("ein Viertel der Torte").parseFirst());
        assertEquals(n(10), pf.extractNumber("zehn plus zwei").parseFirst());
        assertEquals(Long.valueOf(4), pf.extractNumber("vier Stück").parseFirstIfInteger());
    }
}
