package org.dicio.numbers.lang.ru;

import static org.dicio.numbers.test.TestUtils.F;
import static org.junit.Assert.assertEquals;

import org.dicio.numbers.ParserFormatter;
import org.junit.BeforeClass;
import org.junit.Test;

public class NiceNumberTest {

    private static ParserFormatter pf;

    @BeforeClass
    public static void setup() {
        pf = new ParserFormatter(new RussianFormatter(), null);
    }

    @Test
    public void speech() {
        assertEquals("половина", pf.niceNumber(0.5).get());
        assertEquals("два с половиной", pf.niceNumber(2.5).get());
        assertEquals("одна пятая", pf.niceNumber(0.2).get());
        assertEquals("две пятых", pf.niceNumber(0.4).get());
        assertEquals("один и три четвёртых", pf.niceNumber(1.75).get());
        assertEquals("семь девятых", pf.niceNumber(7.0 / 9).get());
        assertEquals("минус восемнадцать и три пятых", pf.niceNumber(-18.6).get());
        assertEquals("одна третья", pf.niceNumber(1.0 / 3).get());
        assertEquals("сорок два", pf.niceNumber(42).get());
    }

    @Test
    public void noSpeech() {
        assertEquals("1 3/4", pf.niceNumber(1.75).speech(F).get());
        assertEquals("42", pf.niceNumber(42).speech(F).get());
    }
}
