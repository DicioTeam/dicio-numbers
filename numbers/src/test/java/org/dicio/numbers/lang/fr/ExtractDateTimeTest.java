package org.dicio.numbers.lang.fr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.dicio.numbers.ParserFormatter;
import org.junit.BeforeClass;
import org.junit.Test;

import java.time.LocalDateTime;

public class ExtractDateTimeTest {

    private static ParserFormatter pf;

    @BeforeClass
    public static void setup() {
        pf = new ParserFormatter(new FrenchFormatter(), new FrenchParser());
    }

    private static final LocalDateTime NOW = LocalDateTime.of(2022, 5, 10, 19, 38, 36);

    @Test
    public void testExtractDateTime() {
        assertEquals(LocalDateTime.of(2021, 4, 28, 19, 38, 36),
                pf.extractDateTime("le vingt-huit avril deux mille vingt et un")
                        .now(NOW).get());
        assertEquals(LocalDateTime.of(2022, 5, 10, 14, 0, 0),
                pf.extractDateTime("à quatorze heures").now(NOW).get());
    }

    @Test
    public void testExtractDateTimeNull() {
        assertNull(pf.extractDateTime("bonjour le monde").now(NOW).get());
    }
}
