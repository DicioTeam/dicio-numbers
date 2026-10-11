package org.dicio.numbers.lang.sv;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.dicio.numbers.ParserFormatter;
import org.dicio.numbers.test.WithTokenizerTestBase;
import org.junit.Test;

import java.time.LocalDateTime;

public class ExtractDateTimeTest extends WithTokenizerTestBase {

    // Tuesday the 10th of May, 2022, 19:38:36
    private static final LocalDateTime NOW = LocalDateTime.of(2022, 5, 10, 19, 38, 36, 295834726);

    @Override
    public String configFolder() {
        return "config/sv-se";
    }

    private LocalDateTime parse(final String s) {
        return new ParserFormatter(null, new SwedishParser())
                .extractDateTime(s).now(NOW).parseFirst();
    }

    @Test
    public void testDateTime() {
        assertNull(parse("hej hur går det"));
        assertEquals(NOW.plusMinutes(3).plusSeconds(46), parse("om tre minuter och fyrtiosex sekunder"));
        assertEquals(NOW.minusDays(30), parse("trettio dagar sedan"));
        assertEquals(NOW.plusDays(1), parse("imorgon"));
        assertEquals(NOW.minusDays(1), parse("igår"));
        assertEquals(NOW.plusDays(2), parse("i övermorgon"));
        assertEquals(NOW.plusHours(2).plusMinutes(20), parse("om två timmar och tjugo minuter"));
        assertEquals(NOW.plusDays(1).withHour(3).withMinute(30).withSecond(0).withNano(0),
                parse("imorgon halv fyra"));
        assertEquals(NOW.withHour(10).withMinute(15).withSecond(0).withNano(0),
                parse("kvart över tio"));
        assertEquals(NOW.withHour(15).withMinute(45).withSecond(0).withNano(0),
                parse("kvart i fyra på eftermiddagen"));
        assertEquals(NOW.withMonth(12).withDayOfMonth(24),
                parse("tjugofjärde december"));
    }
}
