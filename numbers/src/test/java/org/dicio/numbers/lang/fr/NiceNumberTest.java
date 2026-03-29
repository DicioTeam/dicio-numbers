package org.dicio.numbers.lang.fr;

import org.dicio.numbers.ParserFormatter;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.dicio.numbers.test.TestUtils.F;
import static org.junit.Assert.assertEquals;

public class NiceNumberTest {

    private static ParserFormatter pf;

    @BeforeClass
    public static void setup() {
        pf = new ParserFormatter(new FrenchFormatter(), null);
    }

    @Test
    public void speech() {
        assertEquals("trente-quatre et un demi",       pf.niceNumber(34.5).get());
        assertEquals("quatre cents soixante-cinq",     pf.niceNumber(465).get());
        assertEquals("moins quatre-vingt-onze",        pf.niceNumber(-91).get());
        assertEquals("zéro",                           pf.niceNumber(0).get());
    }

    @Test
    public void noSpeech() {
        assertEquals("34 1/2",   pf.niceNumber(34.5).speech(F).get());
        assertEquals("-18 3/5",  pf.niceNumber(-18.6).speech(F).get());
        assertEquals("465",      pf.niceNumber(465).speech(F).get());
        assertEquals("-91",      pf.niceNumber(-91).speech(F).get());
        assertEquals("0",        pf.niceNumber(0).speech(F).get());
    }
}
