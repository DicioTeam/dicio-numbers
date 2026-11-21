package org.dicio.numbers.lang.ru;

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
        pf = new ParserFormatter(new RussianFormatter(), null);
    }

    @Test
    public void random() {
        final LocalTime dt = LocalTime.of(13, 22, 3);
        assertEquals("час двадцать две минуты", pf.niceTime(dt).get());
        assertEquals("час двадцать две минуты дня", pf.niceTime(dt).showAmPm(T).get());
        assertEquals("тринадцать часов двадцать две минуты", pf.niceTime(dt).use24Hour(T).get());
        assertEquals("1:22", pf.niceTime(dt).speech(F).get());
        assertEquals("1:22 дня", pf.niceTime(dt).speech(F).showAmPm(T).get());
        assertEquals("13:22", pf.niceTime(dt).speech(F).use24Hour(T).get());
    }

    @Test
    public void special() {
        assertEquals("полночь", pf.niceTime(LocalTime.of(0, 0)).get());
        assertEquals("полдень", pf.niceTime(LocalTime.of(12, 0)).get());
        assertEquals("ноль часов", pf.niceTime(LocalTime.of(0, 0)).use24Hour(T).get());
        assertEquals("двадцать один час пять минут",
                pf.niceTime(LocalTime.of(21, 5)).use24Hour(T).get());
        assertEquals("девять часов тридцать минут утра",
                pf.niceTime(LocalTime.of(9, 30)).showAmPm(T).get());
        assertEquals("три часа ночи", pf.niceTime(LocalTime.of(3, 0)).showAmPm(T).get());
        assertEquals("восемь часов вечера", pf.niceTime(LocalTime.of(20, 0)).showAmPm(T).get());
    }
}
