package org.dicio.numbers.lang.de;

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
        pf = new ParserFormatter(new GermanFormatter(), null);
    }

    private static LocalTime time(final int hour, final int minute) {
        return LocalTime.of(hour, minute);
    }

    @Test
    public void speech24Hour() {
        assertEquals("dreizehn Uhr zweiundzwanzig", pf.niceTime(time(13, 22)).use24Hour(T).get());
        assertEquals("ein Uhr fünf", pf.niceTime(time(1, 5)).use24Hour(T).get());
        assertEquals("null Uhr", pf.niceTime(time(0, 0)).use24Hour(T).get());
        assertEquals("fünfzehn Uhr", pf.niceTime(time(15, 0)).use24Hour(T).get());
        assertEquals("dreiundzwanzig Uhr neunundfünfzig", pf.niceTime(time(23, 59)).use24Hour(T).get());
    }

    @Test
    public void speech12Hour() {
        assertEquals("ein Uhr zweiundzwanzig", pf.niceTime(time(13, 22)).get());
        assertEquals("halb vier", pf.niceTime(time(15, 30)).get());
        assertEquals("viertel nach drei", pf.niceTime(time(3, 15)).get());
        assertEquals("viertel vor vier", pf.niceTime(time(15, 45)).get());
        assertEquals("halb eins", pf.niceTime(time(0, 30)).get());
        assertEquals("viertel vor zwölf", pf.niceTime(time(11, 45)).get());
        assertEquals("viertel vor eins", pf.niceTime(time(12, 45)).get());
        assertEquals("ein Uhr", pf.niceTime(time(1, 0)).get());
        assertEquals("zwölf Uhr mittags", pf.niceTime(time(12, 0)).get());
        assertEquals("Mitternacht", pf.niceTime(time(0, 0)).get());
    }

    @Test
    public void speech12HourWithMomentOfDay() {
        assertEquals("ein Uhr zweiundzwanzig nachmittags", pf.niceTime(time(13, 22)).showAmPm(T).get());
        assertEquals("acht Uhr morgens", pf.niceTime(time(8, 0)).showAmPm(T).get());
        assertEquals("halb elf vormittags", pf.niceTime(time(10, 30)).showAmPm(T).get());
        assertEquals("neun Uhr abends", pf.niceTime(time(21, 0)).showAmPm(T).get());
        assertEquals("drei Uhr zehn nachts", pf.niceTime(time(3, 10)).showAmPm(T).get());
    }

    @Test
    public void noSpeech() {
        assertEquals("13:22", pf.niceTime(time(13, 22)).speech(F).use24Hour(T).get());
        assertEquals("08:05", pf.niceTime(time(8, 5)).speech(F).use24Hour(T).get());
        assertEquals("1:22", pf.niceTime(time(13, 22)).speech(F).get());
        assertEquals("1:22 PM", pf.niceTime(time(13, 22)).speech(F).showAmPm(T).get());
        assertEquals("12:05", pf.niceTime(time(0, 5)).speech(F).get());
    }
}
