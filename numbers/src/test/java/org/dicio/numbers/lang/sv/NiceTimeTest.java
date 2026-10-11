package org.dicio.numbers.lang.sv;

import static org.dicio.numbers.test.TestUtils.F;
import static org.dicio.numbers.test.TestUtils.T;
import static org.junit.Assert.assertEquals;

import org.dicio.numbers.ParserFormatter;
import org.junit.BeforeClass;
import org.junit.Test;

import java.time.LocalTime;

public class NiceTimeTest {

    private static ParserFormatter pf;

    @BeforeClass
    public static void setup() {
        pf = new ParserFormatter(new SwedishFormatter(), null);
    }

    @Test
    public void random() {
        final LocalTime dt = LocalTime.of(13, 22, 3);
        assertEquals("ett tjugotvå", pf.niceTime(dt).get());
        assertEquals("klockan tretton tjugotvå", pf.niceTime(dt).use24Hour(T).get());
        assertEquals("1:22", pf.niceTime(dt).speech(F).get());
        assertEquals("13:22", pf.niceTime(dt).speech(F).use24Hour(T).get());
    }

    @Test
    public void quarters() {
        assertEquals("kvart över tio", pf.niceTime(LocalTime.of(10, 15)).get());
        assertEquals("halv fyra", pf.niceTime(LocalTime.of(3, 30)).get());
        assertEquals("kvart i fyra", pf.niceTime(LocalTime.of(3, 45)).get());
        assertEquals("halv ett", pf.niceTime(LocalTime.of(12, 30)).get());
        assertEquals("halv ett", pf.niceTime(LocalTime.of(0, 30)).get());
    }

    @Test
    public void noonAndMidnight() {
        assertEquals("tolv", pf.niceTime(LocalTime.of(12, 0)).get());
        assertEquals("midnatt", pf.niceTime(LocalTime.of(0, 0)).get());
    }

    @Test
    public void oClock() {
        final LocalTime dt = LocalTime.of(15, 0, 32);
        assertEquals("tre", pf.niceTime(dt).get());
        assertEquals("klockan femton", pf.niceTime(dt).use24Hour(T).get());
        assertEquals("3:00", pf.niceTime(dt).speech(F).get());
        assertEquals("15:00", pf.niceTime(dt).speech(F).use24Hour(T).get());
    }

    @Test
    public void leadingZeroMinutes() {
        assertEquals("klockan noll noll noll fem", pf.niceTime(LocalTime.of(0, 5)).use24Hour(T).get());
        assertEquals("12:05", pf.niceTime(LocalTime.of(0, 5)).speech(F).get());
        assertEquals("00:05", pf.niceTime(LocalTime.of(0, 5)).speech(F).use24Hour(T).get());
    }

    @Test
    public void amPm() {
        assertEquals("tre på natten", pf.niceTime(LocalTime.of(3, 0)).showAmPm(T).get());
        assertEquals("sju på morgonen", pf.niceTime(LocalTime.of(7, 0)).showAmPm(T).get());
        assertEquals("elva på förmiddagen", pf.niceTime(LocalTime.of(11, 0)).showAmPm(T).get());
        assertEquals("tolv på eftermiddagen", pf.niceTime(LocalTime.of(12, 0)).showAmPm(T).get());
        assertEquals("tre på eftermiddagen", pf.niceTime(LocalTime.of(15, 0)).showAmPm(T).get());
        assertEquals("åtta på kvällen", pf.niceTime(LocalTime.of(20, 0)).showAmPm(T).get());
        assertEquals("midnatt", pf.niceTime(LocalTime.of(0, 0)).showAmPm(T).get());
    }

    @Test
    public void halfPast() {
        final LocalTime pm = LocalTime.of(15, 30);
        assertEquals("halv fyra", pf.niceTime(pm).get());
        assertEquals("halv fyra på eftermiddagen", pf.niceTime(pm).showAmPm(T).get());
        assertEquals("klockan femton trettio", pf.niceTime(pm).use24Hour(T).get());
        assertEquals("3:30", pf.niceTime(pm).speech(F).get());
        assertEquals("15:30", pf.niceTime(pm).speech(F).use24Hour(T).get());

        final LocalTime am = LocalTime.of(3, 30);
        assertEquals("halv fyra", pf.niceTime(am).get());
        assertEquals("halv fyra på natten", pf.niceTime(am).showAmPm(T).get());
        assertEquals("klockan tre trettio", pf.niceTime(am).use24Hour(T).get());
        assertEquals("3:30", pf.niceTime(am).speech(F).get());
        assertEquals("03:30", pf.niceTime(am).speech(F).use24Hour(T).get());
    }
}
