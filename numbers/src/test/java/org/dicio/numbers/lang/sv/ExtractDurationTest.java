package org.dicio.numbers.lang.sv;

import static org.dicio.numbers.test.TestUtils.DAY;
import static org.dicio.numbers.test.TestUtils.HOUR;
import static org.dicio.numbers.test.TestUtils.t;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.dicio.numbers.ParserFormatter;
import org.dicio.numbers.test.WithTokenizerTestBase;
import org.junit.Test;

public class ExtractDurationTest extends WithTokenizerTestBase {
    @Override
    public String configFolder() {
        return "config/sv-se";
    }

    @Test
    public void testExtractDuration() {
        final ParserFormatter npf = new ParserFormatter(null, new SwedishParser());
        assertNull(npf.extractDuration("hej hur går det").parseFirst());
        assertEquals(t(DAY), npf.extractDuration("tjugofyra timmar").parseFirst().toJavaDuration());
        assertEquals(t(2 * DAY), npf.extractDuration("två dagar").parseFirst().toJavaDuration());
        assertEquals(t(2 * HOUR + 20 * 60),
                npf.extractDuration("om två timmar och tjugo minuter").parseFirst().toJavaDuration());
        assertEquals(t(90),
                npf.extractDuration("en minut och trettio sekunder").parseFirst().toJavaDuration());
        assertEquals(t(HOUR + 15 * 60),
                npf.extractDuration("en timme och femton minuter").parseFirst().toJavaDuration());
        assertEquals(t(2 * DAY + 3 * HOUR),
                npf.extractDuration("två dagar och tre timmar").parseFirst().toJavaDuration());
    }
}
