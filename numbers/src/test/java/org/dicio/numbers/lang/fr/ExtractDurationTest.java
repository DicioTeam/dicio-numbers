package org.dicio.numbers.lang.fr;

import static org.dicio.numbers.test.TestUtils.DAY;
import static org.dicio.numbers.test.TestUtils.HOUR;
import static org.dicio.numbers.test.TestUtils.MINUTE;
import static org.dicio.numbers.test.TestUtils.YEAR;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.dicio.numbers.ParserFormatter;
import org.dicio.numbers.unit.Duration;
import org.junit.BeforeClass;
import org.junit.Test;

public class ExtractDurationTest {

    private static ParserFormatter pf;

    @BeforeClass
    public static void setup() {
        pf = new ParserFormatter(new FrenchFormatter(), new FrenchParser());
    }

    @Test
    public void testExtractDuration() {
        assertEquals(new Duration().plus(2 * MINUTE + 30, java.time.temporal.ChronoUnit.SECONDS),
                pf.extractDuration("un minuteur de deux minutes et trente secondes").get());
        assertEquals(new Duration().plus(2 * YEAR, java.time.temporal.ChronoUnit.SECONDS),
                pf.extractDuration("il y a deux ans").get());
        assertEquals(new Duration().plus(23 * HOUR, java.time.temporal.ChronoUnit.SECONDS),
                pf.extractDuration("vingt-trois heures").get());
        assertEquals(new Duration().plus(5 * DAY, java.time.temporal.ChronoUnit.SECONDS),
                pf.extractDuration("cinq jours").get());
    }

    @Test
    public void testExtractDurationNull() {
        assertNull(pf.extractDuration("bonjour le monde").get());
    }
}
