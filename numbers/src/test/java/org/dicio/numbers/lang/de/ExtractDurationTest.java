package org.dicio.numbers.lang.de;

import static org.dicio.numbers.test.TestUtils.DAY;
import static org.dicio.numbers.test.TestUtils.HOUR;
import static org.dicio.numbers.test.TestUtils.MINUTE;
import static org.dicio.numbers.test.TestUtils.MONTH;
import static org.dicio.numbers.test.TestUtils.t;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.dicio.numbers.ParserFormatter;
import org.dicio.numbers.test.WithTokenizerTestBase;
import org.junit.Test;

public class ExtractDurationTest extends WithTokenizerTestBase {
    @Override
    public String configFolder() {
        return "config/de-de";
    }

    @Test
    public void testNumberParserExtractDuration() {
        final ParserFormatter npf = new ParserFormatter(null, new GermanParser());
        assertNull(npf.extractDuration("hallo wie geht es dir").parseFirst());
        assertNull(npf.extractDuration("eine Milliarde Euro").shortScale(true).parseFirst());
        assertNull(npf.extractDuration("eine Million").shortScale(false).parseFirst());
        assertEquals(t(DAY), npf.extractDuration("vierundzwanzig Stunden sind nicht zwei Tage").parseFirst().toJavaDuration());
        assertEquals(t(2 * DAY), npf.extractDuration("zwei Tage sind nicht vierundzwanzig Stunden").parseFirst().toJavaDuration());
        assertEquals(t(3 * MONTH + 2 * DAY), npf.extractDuration("drei Monate und zwei Tage").parseFirst().toJavaDuration());
    }

    @Test
    public void testFractionsAndCompounds() {
        final ParserFormatter npf = new ParserFormatter(null, new GermanParser());
        assertEquals(t(30 * MINUTE), npf.extractDuration("eine halbe Stunde").parseFirst().toJavaDuration());
        assertEquals(t(90 * MINUTE), npf.extractDuration("anderthalb Stunden").parseFirst().toJavaDuration());
        assertEquals(t(150), npf.extractDuration("zweieinhalb Minuten").parseFirst().toJavaDuration());
        assertEquals(t(45 * MINUTE), npf.extractDuration("dreiviertel Stunde").parseFirst().toJavaDuration());
        assertEquals(t(15 * MINUTE), npf.extractDuration("eine Viertelstunde").parseFirst().toJavaDuration());
        assertEquals(t(45 * MINUTE), npf.extractDuration("eine Dreiviertelstunde").parseFirst().toJavaDuration());
        assertEquals(t(5 * MINUTE + 30), npf.extractDuration("fünf Minuten und dreißig Sekunden").parseFirst().toJavaDuration());
        assertEquals(t(2 * HOUR + 30 * MINUTE), npf.extractDuration("2 h 30 min").parseFirst().toJavaDuration());
        assertEquals(t(25 * MINUTE), npf.extractDuration("stell einen Timer auf fünfundzwanzig Minuten").parseFirst().toJavaDuration());
        assertEquals(t(DAY), npf.extractDuration("ein Tag").parseFirst().toJavaDuration());
    }
}
