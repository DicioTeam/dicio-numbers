package org.dicio.numbers.lang.es;

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
        pf = new ParserFormatter(new SpanishFormatter(), null);
    }


    @Test
    public void random() {
        final LocalTime dt = LocalTime.of(13, 22, 3);
        assertEquals("una veintidós", pf.niceTime(dt).get());
        assertEquals("una y veintidós p.m.", pf.niceTime(dt).showAmPm(T).get());
        assertEquals("trece veintidós", pf.niceTime(dt).use24Hour(T).get());
        assertEquals("trece veintidós", pf.niceTime(dt).use24Hour(T).showAmPm(T).get());
        assertEquals("1:22", pf.niceTime(dt).speech(F).get());
        assertEquals("1:22 PM", pf.niceTime(dt).speech(F).showAmPm(T).get());
        assertEquals("13:22", pf.niceTime(dt).speech(F).use24Hour(T).get());
        assertEquals("13:22", pf.niceTime(dt).speech(F).use24Hour(T).showAmPm(T).get());
    }

    @Test
    public void oClock() {
        final LocalTime dt = LocalTime.of(15, 0, 32);
        assertEquals("tres en punto", pf.niceTime(dt).get());
        assertEquals("tres p.m.", pf.niceTime(dt).showAmPm(T).get());
        assertEquals("mil quinientos", pf.niceTime(dt).use24Hour(T).get());
        assertEquals("mil quinientos", pf.niceTime(dt).use24Hour(T).showAmPm(T).get());
        assertEquals("3:00", pf.niceTime(dt).speech(F).get());
        assertEquals("3:00 PM", pf.niceTime(dt).speech(F).showAmPm(T).get());
        assertEquals("15:00", pf.niceTime(dt).speech(F).use24Hour(T).get());
        assertEquals("15:00", pf.niceTime(dt).speech(F).use24Hour(T).showAmPm(T).get());
    }

    @Test
    public void afterMidnight() {
        final LocalTime dt = LocalTime.of(0, 2, 9);
        assertEquals("dos cero dos", pf.niceTime(dt).get());
        assertEquals("doce cero dos a.m.", pf.niceTime(dt).showAmPm(T).get());
        assertEquals("cero cero cero dos", pf.niceTime(dt).use24Hour(T).get());
        assertEquals("cero cero cero dos", pf.niceTime(dt).use24Hour(T).showAmPm(T).get());
        assertEquals("12:02", pf.niceTime(dt).speech(F).get());
        assertEquals("12:02 AM", pf.niceTime(dt).speech(F).showAmPm(T).get());
        assertEquals("00:02", pf.niceTime(dt).speech(F).use24Hour(T).get());
        assertEquals("00:02", pf.niceTime(dt).speech(F).use24Hour(T).showAmPm(T).get());
    }

    @Test
    public void quarterPast() {
        final LocalTime dt = LocalTime.of(1, 15, 33);
        assertEquals("una y cuarto", pf.niceTime(dt).get());
        assertEquals("una y cuarto a.m.", pf.niceTime(dt).showAmPm(T).get());
        assertEquals("cero uno quince", pf.niceTime(dt).use24Hour(T).get());
        assertEquals("cero uno quince", pf.niceTime(dt).use24Hour(T).showAmPm(T).get());
        assertEquals("1:15", pf.niceTime(dt).speech(F).get());
        assertEquals("1:15 AM", pf.niceTime(dt).speech(F).showAmPm(T).get());
        assertEquals("01:15", pf.niceTime(dt).speech(F).use24Hour(T).get());
        assertEquals("01:15", pf.niceTime(dt).speech(F).use24Hour(T).showAmPm(T).get());
    }

    @Test
    public void half() {
        final LocalTime dt = LocalTime.of(12, 30, 59);
        assertEquals("doce y media", pf.niceTime(dt).get());
        assertEquals("doce y media p.m.", pf.niceTime(dt).showAmPm(T).get());
        assertEquals("doce treinta", pf.niceTime(dt).use24Hour(T).get());
        assertEquals("doce y treinta", pf.niceTime(dt).use24Hour(T).showAmPm(T).get());
        assertEquals("12:30", pf.niceTime(dt).speech(F).get());
        assertEquals("12:30 PM", pf.niceTime(dt).speech(F).showAmPm(T).get());
        assertEquals("12:30", pf.niceTime(dt).speech(F).use24Hour(T).get());
        assertEquals("12:30", pf.niceTime(dt).speech(F).use24Hour(T).showAmPm(T).get());
    }

    @Test
    public void quarterTo() {
        final LocalTime dt = LocalTime.of(23, 45, 7);
        assertEquals("cuarto para las doce", pf.niceTime(dt).get());
        assertEquals("cuarto para las doce p.m.", pf.niceTime(dt).showAmPm(T).get());
        assertEquals("veintitrés cuarenta y cinco", pf.niceTime(dt).use24Hour(T).get());
        assertEquals("veintitrés cuarenta y cinco", pf.niceTime(dt).use24Hour(T).showAmPm(T).get());
        assertEquals("11:45", pf.niceTime(dt).speech(F).get());
        assertEquals("11:45 PM", pf.niceTime(dt).speech(F).showAmPm(T).get());
        assertEquals("23:45", pf.niceTime(dt).speech(F).use24Hour(T).get());
        assertEquals("23:45", pf.niceTime(dt).speech(F).use24Hour(T).showAmPm(T).get());
    }

    @Test
    public void tenAm() {
        final LocalTime dt = LocalTime.of(10, 3, 44);
        assertEquals("diez cero tres", pf.niceTime(dt).get());
        assertEquals("diez cero tres a.m.", pf.niceTime(dt).showAmPm(T).get());
        assertEquals("diez cero tres", pf.niceTime(dt).use24Hour(T).get());
        assertEquals("diez y tres", pf.niceTime(dt).use24Hour(T).showAmPm(T).get());
        assertEquals("10:03", pf.niceTime(dt).speech(F).get());
        assertEquals("10:03 AM", pf.niceTime(dt).speech(F).showAmPm(T).get());
        assertEquals("10:03", pf.niceTime(dt).speech(F).use24Hour(T).get());
        assertEquals("10:03", pf.niceTime(dt).speech(F).use24Hour(T).showAmPm(T).get());
    }
}
