package org.dicio.numbers.lang.fr;

import org.dicio.numbers.formatter.Formatter;
import org.dicio.numbers.test.DateTimeTestBase;
import org.junit.Test;

import java.time.LocalDate;

import static org.junit.Assert.assertEquals;

public class DateTimeTest extends DateTimeTestBase {

    @Override
    public String configFolder() {
        return "config/fr-fr";
    }

    @Override
    public Formatter buildNumberFormatter() {
        return new FrenchFormatter();
    }

    @Test
    public void testNiceDate() {
        assertEquals("mercredi, le vingt-huit avril deux mille vingt et un",
                pf.niceDate(LocalDate.of(2021, 4, 28)).get());
        assertEquals("dimanche, le treize août",
                pf.niceDate(LocalDate.of(-84, 8, 13)).now(LocalDate.of(-84, 8, 23)).get());
        assertEquals("hier",
                pf.niceDate(LocalDate.of(2021, 4, 27)).now(LocalDate.of(2021, 4, 28)).get());
        assertEquals("aujourd'hui",
                pf.niceDate(LocalDate.of(2021, 4, 28)).now(LocalDate.of(2021, 4, 28)).get());
        assertEquals("demain",
                pf.niceDate(LocalDate.of(2021, 4, 29)).now(LocalDate.of(2021, 4, 28)).get());
    }
}
