package org.dicio.numbers.lang.de;

import static org.dicio.numbers.test.TestUtils.DAY;
import static org.dicio.numbers.test.TestUtils.t;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.dicio.numbers.NumberParserFormatter;
import org.dicio.numbers.test.WithTokenizerTestBase;
import org.junit.Test;

public class ExtractDurationTest extends WithTokenizerTestBase {
    @Override
    public String configFolder() {
        return "config/de-de";
    }

    @Test
    public void testNumberParserExtractDuration() {
        final NumberParserFormatter npf
                = new NumberParserFormatter(null, new GermanParser());
        assertNull(npf.extractDuration("hallo wie geht's").get());
        assertNull(npf.extractDuration("eine Milliarde Euro").shortScale(true).get());
        assertNull(npf.extractDuration("eine Million").shortScale(false).get());
        assertEquals(t(DAY), npf.extractDuration("vierundzwanzig Stunden sind nicht zwei Tage").get());
        assertEquals(t(2 * DAY), npf.extractDuration("zwei Tage sind nicht vierundzwanzig Stunden").get());
    }
}
