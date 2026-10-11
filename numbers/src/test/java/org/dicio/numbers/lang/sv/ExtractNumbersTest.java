package org.dicio.numbers.lang.sv;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.dicio.numbers.ParserFormatter;
import org.dicio.numbers.test.WithTokenizerTestBase;
import org.dicio.numbers.unit.Number;
import org.junit.Test;

public class ExtractNumbersTest extends WithTokenizerTestBase {

    @Override
    public String configFolder() {
        return "config/sv-se";
    }

    private ParserFormatter pf() {
        return new ParserFormatter(null, new SwedishParser());
    }

    private void assertNum(final String s, final Number expected) {
        assertNum(s, expected, false);
    }

    private void assertNum(final String s, final Number expected, final boolean preferOrdinal) {
        assertEquals("wrong value for " + s, expected, pf().extractNumber(s).preferOrdinal(preferOrdinal).parseFirst());
    }

    @Test
    public void testIntegers() {
        assertNum("noll", new Number(0));
        assertNum("tretton", new Number(13));
        assertNum("tjugotvå", new Number(22));
        assertNum("hundra", new Number(100));
        assertNum("ett hundra tjugotre", new Number(123));
        assertNum("tvåtusen femhundra", new Number(2500));
        assertNum("en miljon", new Number(1000000));
        assertNum("två miljarder", new Number(2000000000000L));
        assertNum("minus fem", new Number(-5));
        assertNum("42", new Number(42));
    }

    @Test
    public void testCommonPhrases() {
        assertNum("tjugotre", new Number(23));
        assertNum("tjugo tre", new Number(23));
        assertNum("nittionio", new Number(99));
        assertNum("hundratjugotre", new Number(123));
        assertNum("etthundra", new Number(100));
        assertNum("ettusen etthundra", new Number(1100));
        assertNum("tre miljoner femhundra tusen", new Number(3500000));
        assertNum("en halv", new Number(0.5));
        assertNum("tre komma fem", new Number(3.5));
        assertNum("3,5", new Number(3.5));
        assertNum("en tredjedel", new Number(1).divide(new Number(3)));
        assertNum("två dussin", new Number(24));
        assertNum("minus tjugo", new Number(-20));
    }

    @Test
    public void testOrdinals() {
        assertNum("tredje", new Number(3).withOrdinal(true));
        assertNum("tjugoförsta", new Number(21).withOrdinal(true), true);
    }

    @Test
    public void testNull() {
        assertNull(pf().extractNumber("hej hur går det").parseFirst());
    }
}
