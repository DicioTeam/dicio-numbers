package org.dicio.numbers.lang.fr;

import org.dicio.numbers.ParserFormatter;
import org.junit.BeforeClass;
import org.junit.Test;

import java.time.LocalTime;

import static org.dicio.numbers.test.TestUtils.F;
import static org.dicio.numbers.test.TestUtils.T;
import static org.junit.Assert.assertEquals;

public class NiceTimeTest {

    private static ParserFormatter pf;

    @BeforeClass
    public static void setup() {
        pf = new ParserFormatter(new FrenchFormatter(), null);
    }

    @Test
    public void random() {
        final LocalTime dt = LocalTime.of(13, 22, 3);
        assertEquals("un heure vingt-deux",                  pf.niceTime(dt).get());
        assertEquals("un heure vingt-deux de l'après-midi",  pf.niceTime(dt).showAmPm(T).get());
        assertEquals("treize heures vingt-deux",              pf.niceTime(dt).use24Hour(T).get());
        assertEquals("treize heures vingt-deux",              pf.niceTime(dt).use24Hour(T).showAmPm(T).get());
        assertEquals("1:22",   pf.niceTime(dt).speech(F).get());
        assertEquals("1:22 PM", pf.niceTime(dt).speech(F).showAmPm(T).get());
        assertEquals("13:22",  pf.niceTime(dt).speech(F).use24Hour(T).get());
        assertEquals("13:22",  pf.niceTime(dt).speech(F).use24Hour(T).showAmPm(T).get());
    }

    @Test
    public void oClock() {
        final LocalTime dt = LocalTime.of(15, 0, 32);
        assertEquals("quinze heures",               pf.niceTime(dt).get());
        assertEquals("quinze heures de l'après-midi", pf.niceTime(dt).showAmPm(T).get());
        assertEquals("quinze heures",               pf.niceTime(dt).use24Hour(T).get());
        assertEquals("3:00",  pf.niceTime(dt).speech(F).get());
        assertEquals("3:00 PM", pf.niceTime(dt).speech(F).showAmPm(T).get());
        assertEquals("15:00", pf.niceTime(dt).speech(F).use24Hour(T).get());
    }

    @Test
    public void noon() {
        final LocalTime dt = LocalTime.of(12, 0, 0);
        assertEquals("midi",   pf.niceTime(dt).get());
        assertEquals("midi",   pf.niceTime(dt).showAmPm(T).get());
        assertEquals("midi",   pf.niceTime(dt).use24Hour(T).get());
        assertEquals("12:00",  pf.niceTime(dt).speech(F).get());
    }

    @Test
    public void midnight() {
        final LocalTime dt = LocalTime.of(0, 0, 0);
        assertEquals("minuit",  pf.niceTime(dt).get());
        assertEquals("minuit",  pf.niceTime(dt).showAmPm(T).get());
        assertEquals("minuit",  pf.niceTime(dt).use24Hour(T).get());
        assertEquals("12:00",   pf.niceTime(dt).speech(F).get());
        assertEquals("12:00 AM", pf.niceTime(dt).speech(F).showAmPm(T).get());
        assertEquals("00:00",   pf.niceTime(dt).speech(F).use24Hour(T).get());
    }

    @Test
    public void quarterPast() {
        final LocalTime dt = LocalTime.of(9, 15, 0);
        assertEquals("neuf heures et quart", pf.niceTime(dt).get());
    }

    @Test
    public void halfPast() {
        final LocalTime dt = LocalTime.of(9, 30, 0);
        assertEquals("neuf heures et demie", pf.niceTime(dt).get());
    }

    @Test
    public void quarterTo() {
        final LocalTime dt = LocalTime.of(9, 45, 0);
        assertEquals("dix heures moins le quart", pf.niceTime(dt).get());
    }
}
