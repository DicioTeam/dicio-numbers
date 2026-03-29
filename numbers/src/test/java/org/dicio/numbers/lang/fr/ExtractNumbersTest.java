package org.dicio.numbers.lang.fr;

import org.dicio.numbers.parser.lexer.TokenStream;
import org.dicio.numbers.test.WithTokenizerTestBase;
import org.dicio.numbers.unit.Number;
import org.junit.Test;

import java.util.function.Function;

import static org.dicio.numbers.test.TestUtils.F;
import static org.dicio.numbers.test.TestUtils.T;
import static org.dicio.numbers.test.TestUtils.numberDeduceType;
import static org.junit.Assert.assertEquals;

public class ExtractNumbersTest extends WithTokenizerTestBase {

    @Override
    public String configFolder() {
        return "config/fr-fr";
    }

    private void assertNumberFunction(final String s,
                                      final Number value,
                                      final int finalTokenStreamPosition,
                                      final Function<FrenchNumberExtractor, Number> numberFunction) {
        final TokenStream ts = new TokenStream(tokenizer.tokenize(s));
        final Number number = numberFunction.apply(new FrenchNumberExtractor(ts));
        assertEquals("wrong value for string " + s, value, number);
        assertEquals("wrong final token position for number " + value,
                finalTokenStreamPosition, ts.position);
    }

    private void assertNumberFunctionNull(final String s,
                                          final Function<FrenchNumberExtractor, Number> numberFunction) {
        assertNumberFunction(s, null, 0, numberFunction);
    }

    private void assertNumberInteger(final String s, final boolean allowOrdinal,
                                     final double value, final boolean isOrdinal,
                                     final int finalTokenStreamPosition) {
        assertNumberFunction(s, numberDeduceType(value).withOrdinal(isOrdinal),
                finalTokenStreamPosition, (enp) -> enp.numberInteger(allowOrdinal));
    }

    private void assertNumberIntegerNull(final String s) {
        assertNumberFunctionNull(s, (enp) -> enp.numberInteger(F));
    }

    @Test
    public void testNumberInteger() {
        assertNumberInteger("deux mille vingt et un",      F, 2021,   F, 5);
        assertNumberInteger("cent soixante-quatre",        F, 164,    F, 2);
        assertNumberInteger("neuf cent dix",               T, 910,    F, 3);
        assertNumberInteger("soixante-dix",                F, 70,     F, 1);
        assertNumberInteger("quatre-vingt-dix-neuf",       F, 99,     F, 1);
        assertNumberInteger("soixante et onze",            F, 71,     F, 3);
        assertNumberInteger("quatre-vingts",               F, 80,     F, 1);
        assertNumberInteger("quatre-vingt-trois",          F, 83,     F, 1);
        assertNumberInteger("premier",                     T, 1,      T, 1);
        assertNumberIntegerNull("premier");
        assertNumberIntegerNull("bonjour");
    }

    @Test
    public void testSpecialFrenchNumbers() {
        // Test soixante + teen = 70-79
        assertNumberInteger("soixante dix",    F, 70,  F, 2);
        assertNumberInteger("soixante onze",   F, 71,  F, 2);
        assertNumberInteger("soixante douze",  F, 72,  F, 2);
        // Test quatre + vingt = 80
        assertNumberInteger("quatre vingts",   F, 80,  F, 2);
        assertNumberInteger("quatre vingt un", F, 81,  F, 3);
        assertNumberInteger("quatre vingt dix", F, 90, F, 3);
    }
}
