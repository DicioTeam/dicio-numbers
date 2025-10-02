package org.dicio.numbers.lang.de;

import static org.dicio.numbers.test.TestUtils.DAY;
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
        final ParserFormatter npf
                = new ParserFormatter(null, null);
        assertNull(npf.extractDuration("hallo wie geht's").getFirst());
        assertNull(npf.extractDuration("eine Milliarde Euro").shortScale(true).getFirst());
        assertNull(npf.extractDuration("eine Million").shortScale(false).getFirst());
        assertEquals(t(DAY), npf.extractDuration("vierundzwanzig Stunden sind nicht zwei Tage").getFirst().toJavaDuration());
        assertEquals(t(2 * DAY), npf.extractDuration("zwei Tage sind nicht vierundzwanzig Stunden").getFirst().toJavaDuration());
    }
}
