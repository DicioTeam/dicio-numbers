package org.dicio.numbers.lang.de;

import org.dicio.numbers.NumberParserFormatter;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.dicio.numbers.test.TestUtils.F;
import static org.junit.Assert.assertEquals;

public class NiceNumberTest {

    private static NumberParserFormatter pf;

    @BeforeClass
    public static void setup() {
        pf = new NumberParserFormatter(new GermanFormatter(), null);
    }

    @Test
    public void speech() {
        assertEquals("vierunddreißig und einhalb", pf.niceNumber(34.5).get());
        assertEquals("minus achtzehn und drei Fünftel", pf.niceNumber(-18.6).get());
        assertEquals("achtundneunzig und achtzehn Neunzehntel", pf.niceNumber(98.947368421).get());
        assertEquals("minus fünf und sechs Elftel", pf.niceNumber(-5.5454545).get());
        assertEquals("sieben Neuntel", pf.niceNumber(7.0 / 9).get());
        assertEquals("minus zwei Siebzehntel", pf.niceNumber(-2.0 / 17).get());
        assertEquals("vierhundertfünfundsechzig", pf.niceNumber(465).get());
        assertEquals("minus einundneunzig", pf.niceNumber(-91).get());
        assertEquals("null", pf.niceNumber(0).get());
    }

    @Test
    public void noSpeech() {
        assertEquals("34 1/2", pf.niceNumber(34.5).speech(F).get());
        assertEquals("-18 3/5", pf.niceNumber(-18.6).speech(F).get());
        assertEquals("98 18/19", pf.niceNumber(98.947368421).speech(F).get());
        assertEquals("-5 6/11", pf.niceNumber(-5.5454545).speech(F).get());
        assertEquals("7/9", pf.niceNumber(7.0 / 9).speech(F).get());
        assertEquals("-2/17", pf.niceNumber(-2.0 / 17).speech(F).get());
        assertEquals("465", pf.niceNumber(465).speech(F).get());
        assertEquals("-91", pf.niceNumber(-91).speech(F).get());
        assertEquals("0", pf.niceNumber(0).speech(F).get());
    }

    @Test
    public void customDenominators() {
        assertEquals("minus vier und vier Zehntel", pf.niceNumber(-4.4).denominators(Arrays.asList(2, 3, 4, 6, 7, 8, 9, 10, 11)).get());
        assertEquals("-64 6/12", pf.niceNumber(-64.5).speech(F).denominators(Collections.singletonList(12)).get());
        assertEquals("minus drei und fünfhunderttausend Millionstel", pf.niceNumber(-3.5).denominators(Arrays.asList(1000000, 2000000)).get());
        assertEquals("9 1000000/2000000", pf.niceNumber(9.5).speech(F).denominators(Arrays.asList(2000000, 1000000)).get());
        assertEquals("null komma acht", pf.niceNumber(4.0 / 5).denominators(Arrays.asList(2, 3, 4)).get());
    }

    @Test
    public void invalidFraction() {
        assertEquals("eins komma acht vier", pf.niceNumber(1.837).get());
        assertEquals("minus achtunddreißig komma eins neun", pf.niceNumber(-38.192).get());
        assertEquals("3829.48", pf.niceNumber(3829.47832).speech(F).get());
        assertEquals("-7.19", pf.niceNumber(-7.1928).speech(F).get());
        assertEquals("-9322.38", pf.niceNumber(-9322 - 8.0 / 21).speech(F).get());
    }
}
