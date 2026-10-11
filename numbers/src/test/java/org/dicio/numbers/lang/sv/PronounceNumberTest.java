package org.dicio.numbers.lang.sv;

import static org.junit.Assert.assertEquals;

import org.dicio.numbers.ParserFormatter;
import org.junit.BeforeClass;
import org.junit.Test;

public class PronounceNumberTest {

    private static ParserFormatter pf;

    @BeforeClass
    public static void setup() {
        pf = new ParserFormatter(new SwedishFormatter(), null);
    }

    @Test
    public void smallIntegers() {
        assertEquals("noll", pf.pronounceNumber(0).get());
        assertEquals("ett", pf.pronounceNumber(1).get());
        assertEquals("tio", pf.pronounceNumber(10).get());
        assertEquals("femton", pf.pronounceNumber(15).get());
        assertEquals("tjugo", pf.pronounceNumber(20).get());
        assertEquals("tjugosju", pf.pronounceNumber(27).get());
        assertEquals("tjugotre", pf.pronounceNumber(23).get());
        assertEquals("nittionio", pf.pronounceNumber(99).get());
    }

    @Test
    public void negativeIntegers() {
        assertEquals("minus ett", pf.pronounceNumber(-1).get());
        assertEquals("minus tjugosju", pf.pronounceNumber(-27).get());
    }

    @Test
    public void hundreds() {
        assertEquals("hundra", pf.pronounceNumber(100).get());
        assertEquals("hundraett", pf.pronounceNumber(101).get());
        assertEquals("hundratjugotre", pf.pronounceNumber(123).get());
        assertEquals("tvåhundra", pf.pronounceNumber(200).get());
        assertEquals("femhundra", pf.pronounceNumber(500).get());
        assertEquals("niohundranittionio", pf.pronounceNumber(999).get());
    }

    @Test
    public void thousands() {
        assertEquals("tusen", pf.pronounceNumber(1000).get());
        assertEquals("ettusen ett", pf.pronounceNumber(1001).get());
        assertEquals("ettusen etthundra", pf.pronounceNumber(1100).get());
        assertEquals("ettusen tvåhundratrettiofyra", pf.pronounceNumber(1234).get());
        assertEquals("tvåtusen", pf.pronounceNumber(2000).get());
        assertEquals("tretusen femhundra", pf.pronounceNumber(3500).get());
        assertEquals("tjugotretusen", pf.pronounceNumber(23000).get());
        assertEquals("etthundratjugotretusen fyrahundrafemtiosex", pf.pronounceNumber(123456).get());
    }

    @Test
    public void bigNumbers() {
        assertEquals("en miljon", pf.pronounceNumber(1e6).get());
        assertEquals("två miljoner", pf.pronounceNumber(2e6).get());
        assertEquals("en miljon ettusen", pf.pronounceNumber(1001000).get());
        assertEquals("en miljard", pf.pronounceNumber(1e9).get());
        assertEquals("tjugoen miljarder", pf.pronounceNumber(21e9).get());
        assertEquals("en biljon", pf.pronounceNumber(1e12).get());
    }

    @Test
    public void decimals() {
        assertEquals("noll komma noll fem", pf.pronounceNumber(0.05).get());
        assertEquals("minus noll komma noll fem", pf.pronounceNumber(-0.05).get());
        assertEquals("ett komma två tre", pf.pronounceNumber(1.234).get());
        assertEquals("tjugoett komma två sex fyra", pf.pronounceNumber(21.264).places(4).get());
        assertEquals("tjugoett komma två sex", pf.pronounceNumber(21.264).places(2).get());
        assertEquals("tjugoett komma tre", pf.pronounceNumber(21.264).places(1).get());
        assertEquals("tjugoett", pf.pronounceNumber(21.264).places(0).get());
    }

    @Test
    public void ordinals() {
        assertEquals("första", pf.pronounceNumber(1).ordinal(true).get());
        assertEquals("tredje", pf.pronounceNumber(3).ordinal(true).get());
        assertEquals("tjugonde", pf.pronounceNumber(20).ordinal(true).get());
        assertEquals("tjugoandra", pf.pronounceNumber(22).ordinal(true).get());
        assertEquals("hundrade", pf.pronounceNumber(100).ordinal(true).get());
        assertEquals("tvåhundrade", pf.pronounceNumber(200).ordinal(true).get());
        assertEquals("tusende", pf.pronounceNumber(1000).ordinal(true).get());
        assertEquals("tvåtusende", pf.pronounceNumber(2000).ordinal(true).get());
        assertEquals("miljonte", pf.pronounceNumber(1e6).ordinal(true).get());
    }

    @Test
    public void niceNumber() {
        assertEquals("tjugotvå", pf.niceNumber(22).get());
        assertEquals("tre och en halv", pf.niceNumber(3.5).get());
    }
}
