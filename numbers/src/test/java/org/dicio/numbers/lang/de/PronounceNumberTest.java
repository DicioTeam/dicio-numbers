package org.dicio.numbers.lang.de;

import static org.dicio.numbers.test.TestUtils.T;
import static org.junit.Assert.assertEquals;

import org.dicio.numbers.ParserFormatter;
import org.junit.BeforeClass;
import org.junit.Test;

public class PronounceNumberTest {

    private static ParserFormatter pf;

    @BeforeClass
    public static void setup() {
        pf = new ParserFormatter(new GermanFormatter(), null);
    }

    @Test
    public void smallIntegers() {
        assertEquals("null", pf.pronounceNumber(0).get());
        assertEquals("eins", pf.pronounceNumber(1).get());
        assertEquals("zehn", pf.pronounceNumber(10).get());
        assertEquals("sechzehn", pf.pronounceNumber(16).get());
        assertEquals("siebzehn", pf.pronounceNumber(17).get());
        assertEquals("zwanzig", pf.pronounceNumber(20).get());
        assertEquals("einundzwanzig", pf.pronounceNumber(21).get());
        assertEquals("siebenundzwanzig", pf.pronounceNumber(27).get());
        assertEquals("dreißig", pf.pronounceNumber(30).get());
        assertEquals("neunundneunzig", pf.pronounceNumber(99).get());
    }

    @Test
    public void negativeSmallIntegers() {
        assertEquals("minus eins", pf.pronounceNumber(-1).get());
        assertEquals("minus fünfundvierzig", pf.pronounceNumber(-45).get());
    }

    @Test
    public void hundreds() {
        assertEquals("einhundert", pf.pronounceNumber(100).get());
        assertEquals("einhunderteins", pf.pronounceNumber(101).get());
        assertEquals("einhundertzehn", pf.pronounceNumber(110).get());
        assertEquals("einhunderteinundzwanzig", pf.pronounceNumber(121).get());
        assertEquals("vierhundertsechsundfünfzig", pf.pronounceNumber(456).get());
        assertEquals("neunhundertneunundneunzig", pf.pronounceNumber(999).get());
    }

    @Test
    public void thousands() {
        assertEquals("eintausend", pf.pronounceNumber(1000).get());
        assertEquals("eintausendeins", pf.pronounceNumber(1001).get());
        assertEquals("zweitausendeinundzwanzig", pf.pronounceNumber(2021).get());
        assertEquals("zwölftausenddreihundertfünfundvierzig", pf.pronounceNumber(12345).get());
        assertEquals("einhunderttausend", pf.pronounceNumber(100000).get());
        assertEquals("neunhundertneunundneunzigtausendneunhundertneunundneunzig", pf.pronounceNumber(999999).get());
    }

    @Test
    public void bigNumbers() {
        assertEquals("eine Million", pf.pronounceNumber(1000000).get());
        assertEquals("zwei Millionen", pf.pronounceNumber(2000000).get());
        assertEquals("eine Million zweihunderttausend", pf.pronounceNumber(1200000).get());
        assertEquals("drei Milliarden eins", pf.pronounceNumber(3000000001L).get());
        assertEquals("eine Billion", pf.pronounceNumber(1e12).get());
        assertEquals("einundzwanzig Millionen dreihunderttausendvierhundert", pf.pronounceNumber(21300400).get());
    }

    @Test
    public void decimals() {
        assertEquals("eins komma fünf", pf.pronounceNumber(1.5).get());
        assertEquals("drei komma eins vier", pf.pronounceNumber(3.14159).get());
        assertEquals("drei komma eins vier zwei", pf.pronounceNumber(3.14159).places(3).get());
        assertEquals("minus null komma zwei fünf", pf.pronounceNumber(-0.25).get());
        assertEquals("null komma null fünf", pf.pronounceNumber(0.05).get());
        assertEquals("zwei", pf.pronounceNumber(1.999).get());
    }

    @Test
    public void ordinals() {
        assertEquals("erste", pf.pronounceNumber(1).ordinal(T).get());
        assertEquals("dritte", pf.pronounceNumber(3).ordinal(T).get());
        assertEquals("siebte", pf.pronounceNumber(7).ordinal(T).get());
        assertEquals("neunzehnte", pf.pronounceNumber(19).ordinal(T).get());
        assertEquals("zwanzigste", pf.pronounceNumber(20).ordinal(T).get());
        assertEquals("einundzwanzigste", pf.pronounceNumber(21).ordinal(T).get());
        assertEquals("hundertste", pf.pronounceNumber(100).ordinal(T).get());
        assertEquals("einhunderterste", pf.pronounceNumber(101).ordinal(T).get());
        assertEquals("einhundertzweite", pf.pronounceNumber(102).ordinal(T).get());
        assertEquals("tausendste", pf.pronounceNumber(1000).ordinal(T).get());
        assertEquals("zweitausendste", pf.pronounceNumber(2000).ordinal(T).get());
        assertEquals("millionste", pf.pronounceNumber(1000000).ordinal(T).get());
        assertEquals("zweimillionste", pf.pronounceNumber(2000000).ordinal(T).get());
        assertEquals("zwei Milliarden dreimillionste", pf.pronounceNumber(2003000000L).ordinal(T).get());
        assertEquals("eine Million zweite", pf.pronounceNumber(1000002).ordinal(T).get());
    }

    @Test
    public void specialNumbers() {
        assertEquals("unendlich", pf.pronounceNumber(Double.POSITIVE_INFINITY).get());
        assertEquals("minus unendlich", pf.pronounceNumber(Double.NEGATIVE_INFINITY).get());
        assertEquals("keine Zahl", pf.pronounceNumber(Double.NaN).get());
        assertEquals("eins komma zwei drei mal zehn hoch sechs",
                pf.pronounceNumber(1234567).scientific(T).get());
        assertEquals("minus fünf mal zehn hoch minus drei",
                pf.pronounceNumber(-0.005).scientific(T).get());
    }
}
