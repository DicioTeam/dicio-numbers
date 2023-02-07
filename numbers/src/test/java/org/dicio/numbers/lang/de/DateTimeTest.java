package org.dicio.numbers.lang.de;

import org.dicio.numbers.formatter.NumberFormatter;
import org.dicio.numbers.test.DateTimeTestBase;
import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.Assert.assertEquals;

public class DateTimeTest extends DateTimeTestBase {

    @Override
    public String configFolder() {
        return "config/de-de";
    }

    @Override
    public NumberFormatter buildNumberFormatter() {
        return new GermanFormatter();
    }

    @Test
    public void testNiceDate() {
        // just check that the NumberParserFormatter functions do their job
        assertEquals("Mittwoch, der achtundzwangzigste April zweitausendeinundzwanzig",
                pf.niceDate(LocalDate.of(2021, 4, 28)).get());
        assertEquals("Sonntag, der dreizehnte August",
                pf.niceDate(LocalDate.of(-84, 8, 13)).now(LocalDate.of(-84, 8, 23)).get());
    }

    @Test
    public void testNiceYear() {
        // just check that the NumberParserFormatter functions do their job
        assertEquals("neunzehnhundertvierundachtzig", pf.niceYear(LocalDate.of(1984, 4, 28)).get());
        assertEquals("achhundertzehn v.d.Z.", pf.niceYear(LocalDate.of(-810, 8, 13)).get());
    }

    @Test
    public void testNiceDateTime() {
        // just check that the NumberParserFormatter functions do their job
        assertEquals("Mittwoch, zwölfter September siebzehnhundertvierundsechzig, um zwölf Uhr mittags", pf.niceDateTime(LocalDateTime.of(1764, 9, 12, 12, 0)).get());
        assertEquals("Donnerstag, dritter November dreihundertachtundzwanzig v.d.Z. um fünf Uhr sieben", pf.niceDateTime(LocalDateTime.of(-328, 11, 3, 5, 7)).get());
    }
}
