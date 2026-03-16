package org.dicio.numbers.lang.fr;

import org.dicio.numbers.formatter.NumberFormatter;
import org.dicio.numbers.test.DateTimeTestBase;
import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.Assert.assertEquals;

public class DateTimeTest extends DateTimeTestBase {

    @Override
    public String configFolder() {
        return "config/fr-fr";
    }

    @Override
    public NumberFormatter buildNumberFormatter() {
        return new FrenchFormatter();
    }

    @Test
    public void testNiceDate() {
        // just check that the NumberParserFormatter functions do their job
        assertEquals("mercredi vingt-huit avril deux mille un",
                pf.niceDate(LocalDate.of(2021, 4, 28)).get());
        assertEquals("dimanche treize août",
                pf.niceDate(LocalDate.of(-84, 8, 13)).now(LocalDate.of(-84, 8, 23)).get());
    }

    /*  Please note that there is two ways of saying years and centuries before 2000. For example:
        1. mille (thousand) neuf (nine) cent (hundred) quatre-vingt (90) quatre (4)
        2. dix-neuf (nineteen) cent (hundred) quatre-vingt (90) quatre (4). (Slightly old-fashioned but common for years before 1900)
    */

    @Test
    public void testNiceYear() {
        // just check that the NumberParserFormatter functions do their job
        assertEquals("mille neuf cent quatre-vingt quatre", pf.niceYear(LocalDate.of(1984, 4, 28)).get());
        assertEquals("mille huit cent dix av. J-C", pf.niceYear(LocalDate.of(-810, 8, 13)).get());
    }

    @Test
    public void testNiceDateTime() {
        // just check that the NumberParserFormatter functions do their job
        assertEquals("mercredi douze septembre mille sept cent soixante quatre à midi", pf.niceDateTime(LocalDateTime.of(1764, 9, 12, 12, 0)).get());
        assertEquals("jeudi trois novembre trois cent vingt huit av. J-C à cinq heures sept", pf.niceDateTime(LocalDateTime.of(-328, 11, 3, 5, 7)).get());
    }
}
