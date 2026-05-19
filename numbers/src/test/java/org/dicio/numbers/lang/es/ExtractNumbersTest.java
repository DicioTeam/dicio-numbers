package org.dicio.numbers.lang.es;

import org.dicio.numbers.ParserFormatter;
import org.dicio.numbers.parser.lexer.TokenStream;
import org.dicio.numbers.test.WithTokenizerTestBase;
import org.dicio.numbers.unit.Number;
import org.junit.Test;

import java.util.function.BiFunction;

import static org.dicio.numbers.test.TestUtils.F;
import static org.dicio.numbers.test.TestUtils.T;
import static org.dicio.numbers.test.TestUtils.n;
import static org.dicio.numbers.test.TestUtils.numberDeduceType;
import static org.junit.Assert.*;

public class ExtractNumbersTest extends WithTokenizerTestBase {

    @Override
    public String configFolder() {
        return "config/es-es";
    }

    private void assertNumberFunction(final String s,
                                      final Number value,
                                      final int finalTokenStreamPosition,
                                      final BiFunction<SpanishNumberExtractor, TokenStream, Number> numberFunction) {
        final TokenStream ts = new TokenStream(tokenizer.tokenize(s));
        // NOTE (ES): SpanishNumberExtractor does not use the shortScale parameter.
        final Number number = numberFunction.apply(new SpanishNumberExtractor(ts), ts);
        assertEquals("wrong value for string \"" + s + "\"", value, number);
        assertEquals("wrong final token position for number " + (value != null ? value.toString() : "null"), finalTokenStreamPosition,
                ts.position);
    }

    private void assertNumberFunctionNull(final String s,
                                          final BiFunction<SpanishNumberExtractor, TokenStream, Number> numberFunction) {
        assertNumberFunction(s, null, 0, numberFunction);
    }

    private void assertNumberInteger(final String s, final boolean allowOrdinal, final double value, final boolean isOrdinal, final int finalTokenStreamPosition) {
        assertNumberFunction(s, numberDeduceType(value).withOrdinal(isOrdinal), finalTokenStreamPosition,
                (enp, ts) -> enp.numberInteger(allowOrdinal));
    }

    private void assertNumberIntegerNull(final String s, final boolean allowOrdinal) {
        assertNumberFunctionNull(s, (enp, ts) -> enp.numberInteger(allowOrdinal));
    }

    private void assertNumberPoint(final String s, final boolean allowOrdinal, final double value, final boolean isOrdinal, final int finalTokenStreamPosition) {
        assertNumberFunction(s, numberDeduceType(value).withOrdinal(isOrdinal),
                finalTokenStreamPosition, (enp, ts) -> enp.numberPoint(allowOrdinal));
    }

    private void assertNumberPointNull(final String s, final boolean allowOrdinal) {
        assertNumberFunctionNull(s, (enp, ts) -> enp.numberPoint(allowOrdinal));
    }

    private void assertNumberSignPoint(final String s, final boolean allowOrdinal, final double value, final boolean isOrdinal, final int finalTokenStreamPosition) {
        assertNumberFunction(s, numberDeduceType(value).withOrdinal(isOrdinal),
                finalTokenStreamPosition, (enp, ts) -> enp.numberSignPoint(allowOrdinal));
    }

    private void assertNumberSignPointNull(final String s, final boolean allowOrdinal) {
        assertNumberFunctionNull(s, (enp, ts) -> enp.numberSignPoint(allowOrdinal));
    }

    private void assertDivideByDenominatorIfPossible(final String s, final Number startingNumber, final Number value, final int finalTokenStreamPosition) {
        assertNumberFunction(s, value, finalTokenStreamPosition,
                (enp, ts) -> enp.divideByDenominatorIfPossible(startingNumber));
    }
    
    // --- Spanish-specific number tests ---

    @Test
    public void testNumberInteger() {
        // NOTE (ES): Spanish uses long scale. Billón = 10^12, Trillón = 10^18.
        assertNumberInteger("veinticinco billones, ciento sesenta y cuatro mil millones, siete mil diecinueve", F, 25164000007019L, F, 11);
        assertNumberInteger("dos mil ciento noventa y uno", F, 2191, F, 5);
        assertNumberInteger("novecientos diez", T, 910, F, 2);
        assertNumberInteger("dos millones", F, 2000000, F, 2);
        assertNumberInteger("un millón", F, 1000000, F, 2);
        assertNumberInteger("mil diez", T, 1010, F, 2);
        assertNumberInteger("1234567890123", T, 1234567890123L, F, 1);
        assertNumberInteger("seiscientos cincuenta y cuatro y", F, 654, F, 4);
        assertNumberInteger("ciento cuatro,", F, 104, F, 2);
        assertNumberInteger("nueve mil, tres millones", T, 9003, F, 4); // "mil" acts as a separator here
    }

    @Test
    public void testNumberIntegerOrdinal() {
        assertNumberInteger("vigésimo quinto", T, 25, T, 2);
        assertNumberInteger("milésimo", T, 1000, T, 1);
        assertNumberInteger("ciento cuatro mil, seis billonésimo", T, 104000e12, T, 5);
        assertNumberInteger("543789º", T, 543789, T, 2);
        assertNumberInteger("75.483.543ro", T, 75483543, T, 6);
        assertNumberIntegerNull("2938ro", F); // Ordinal suffix only works on single token raw numbers
    }

    @Test
    public void testNumberIntegerThousandSeparator() {
        // NOTE (ES): Spanish uses a dot (.) as a thousand separator.
        assertNumberInteger("23.001", T, 23001, F, 3);
        assertNumberInteger("19.123", T, 19123, F, 3);
        assertNumberInteger("un 167.42", F, 167, F, 2);
        assertNumberInteger("1.234.023.054, hola", F, 1234023054, F, 7);
    }
    
    @Test
    public void testNumberIntegerComposition() {
        // NOTE (ES): These tests validate the `compound_word_piece` logic.
        assertNumberInteger("veinte y uno mil", F, 21000, F, 4);
        assertNumberInteger("doscientos mil", F, 200000, F, 2);
        assertNumberInteger("trescientos treinta y tres mil trescientos treinta y tres", F, 333333, F, 8);
        assertNumberInteger("un millón un", F, 1000001, F, 3);
    }

    private int tokensInFormattedString(final String formatted) {
        int tokensInFormatted = 0;
        if (!formatted.isEmpty()) {
            tokensInFormatted = 1;
            for (char c : formatted.toCharArray()) {
                if (c == ' ' || c == ',') {
                    tokensInFormatted++;
                }
            }
        }
        return tokensInFormatted;
    }

    @Test
    public void testNumberIntegerWithFormatter() {
        final ParserFormatter npf = new ParserFormatter(new SpanishFormatter(), null);
        for (int i = 0; i < 2000000;) {
            if (i < 2200) i++;
            else if (i < 100000) i += 1207;
            else i += 299527;

            String formatted = npf.pronounceNumber(i).places(0).get();
            int tokensInFormatted = tokensInFormattedString(formatted);
            assertNumberInteger(formatted, T, i, F, tokensInFormatted);

            formatted = npf.pronounceNumber(i).places(0).ordinal(T).get();
            tokensInFormatted = tokensInFormattedString(formatted);
            assertNumberInteger(formatted, T, i, T, tokensInFormatted);
        }
    }

    @Test
    public void testNumberIntegerNull() {
        assertNumberIntegerNull("", T);
        assertNumberIntegerNull("un hola cómo estás", F);
        assertNumberIntegerNull(", y", T);
        assertNumberIntegerNull("cero dos", F);
        assertNumberIntegerNull(", 123.485 y", T);
        assertNumberIntegerNull("y 123", F);
        assertNumberIntegerNull(" un mil ", T);
    }

    @Test
    public void testNumberPoint() {
        // NOTE (ES): Uses "coma" as a decimal point.
        assertNumberPoint("mil quinientos setenta y cuatro coma nueve uno dos cero", T, 1574.9120, F, 9);
        assertNumberPoint("veintitrés coma cero uno cero dos tres", T, 23.01023, F, 7);
        assertNumberPoint("3645,7183", T, 3645.7183, F, 3);
        assertNumberPoint("ochenta coma 6745", T, 80.6745, F, 3);
        assertNumberPoint("cuatro coma sesenta y siete", T, 4.67, F, 4);
        assertNumberPoint("coma ochocientos", T, 0.8, F, 2);
    }

    @Test
    public void testNumberPointFraction() {
        assertNumberPoint("veintitrés millones cien mil sesenta y cuatro sobre dieciséis", F, 1443754, F, 9);
        assertNumberPoint("ocho mil ciento noventa y dos dividido por cuatro mil noventa y seis", T, 2, F, 11);
        assertNumberPoint("noventa y ocho sobre cien", T, 0.98, F, 5);
        assertNumberPoint("veinticuatro sobre sesenta y cinco", T, 24.0 / 65.0, F, 5);
    }

    @Test
    public void testNumberPointOrdinal() {
        assertNumberPoint("quinto coma seis", T, 5, T, 1);
        assertNumberPoint("ocho coma un segundo", F, 8.1, F, 4);
        assertNumberPoint("nueve sobre trigésimo noveno", T, 9.0/39.0, F, 4);
    }

    @Test
    public void testNumberPointNull() {
        assertNumberPointNull("", F);
        assertNumberPointNull("hola mundo", T);
        assertNumberPointNull("coma", F);
        assertNumberPointNull("coma veinte", T);
        assertNumberPointNull("sobre dos", F);
        assertNumberPointNull(" uno dividido por cinco", T);
    }

    @Test
    public void testNumberSignPoint() {
        assertNumberSignPoint("menos setenta y seis mil sobre 23", T, -76000.0 / 23.0, F, 6);
        assertNumberSignPoint("menos doce", T, -12, F, 2);
        assertNumberSignPoint("más un millón", T, 1000000, F, 3);
        assertNumberSignPoint("-1843", F, -1843, F, 2);
        assertNumberSignPoint("+573.976", T, 573976, F, 4);
        assertNumberSignPoint("menos 42903,5", T, -42903.5, F, 4);
        assertNumberSignPoint("menos coma cero cuatro", T, -0.04, F, 4);
    }

    @Test
    public void testNumberSignPointOrdinal() {
        assertNumberSignPoint("menos duodécimo", T, -12, T, 2);
        assertNumberSignPoint("-centésimo", F, -100, T, 2);
        assertNumberSignPointNull("menos primero", F);
    }

    @Test
    public void testDivideByDenominatorIfPossible() {
        assertDivideByDenominatorIfPossible("quintos", n(5, F), n(1, F), 1);
        assertDivideByDenominatorIfPossible("docena dos", n(3, F), n(36, F), 2);
        assertDivideByDenominatorIfPossible("media y", n(19, F), n(9.5, F), 2);
        assertDivideByDenominatorIfPossible("%", n(50, F), n(0.5, F), 1);
        assertDivideByDenominatorIfPossible("‰", n(1000, F), n(1, F), 1);
        assertDivideByDenominatorIfPossible("cuarto", n(16, F), n(4, F), 1);
        assertDivideByDenominatorIfPossible("gente", n(98, F), n(98, F), 0);
        assertDivideByDenominatorIfPossible("un décimo", null, n(0.1, F), 2);
        assertDivideByDenominatorIfPossible("una decena", null, null, 0);
    }
}