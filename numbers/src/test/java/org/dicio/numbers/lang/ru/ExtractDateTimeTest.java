package org.dicio.numbers.lang.ru;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.dicio.numbers.ParserFormatter;
import org.junit.BeforeClass;
import org.junit.Test;

import java.time.LocalDateTime;

public class ExtractDateTimeTest {

    // Tuesday the 10th of May, 2022, 19:38:36
    private static final LocalDateTime NOW = LocalDateTime.of(2022, 5, 10, 19, 38, 36, 295834726);

    private static ParserFormatter pf;

    @BeforeClass
    public static void setup() {
        pf = new ParserFormatter(null, new RussianParser());
    }

    private static void assertDateTime(final LocalDateTime expected, final String s) {
        assertEquals("wrong date time for \"" + s + "\"", expected,
                pf.extractDateTime(s).now(NOW).parseFirst());
    }

    private static LocalDateTime nowAt(final int year, final int month, final int day) {
        return NOW.withYear(year).withMonth(month).withDayOfMonth(day);
    }

    private static LocalDateTime at(final int month, final int day, final int hour, final int minute) {
        return LocalDateTime.of(2022, month, day, hour, minute);
    }

    @Test
    public void relativeDays() {
        assertDateTime(nowAt(2022, 5, 11), "завтра");
        assertDateTime(nowAt(2022, 5, 9), "вчера");
        assertDateTime(nowAt(2022, 5, 8), "позавчера");
        assertDateTime(nowAt(2022, 5, 13), "через три дня");
        assertDateTime(nowAt(2022, 5, 8), "два дня назад");
        assertDateTime(nowAt(2022, 5, 16), "в следующий понедельник");
    }

    @Test
    public void relativeTimes() {
        assertDateTime(NOW.plusHours(2), "через 2 часа");
        assertDateTime(NOW.plusMinutes(30), "через полчаса");
        assertDateTime(NOW.plusMinutes(1), "через минуту");
    }

    @Test
    public void dates() {
        assertDateTime(nowAt(2022, 6, 5), "пятого июня");
        assertDateTime(nowAt(2022, 6, 5), "5 июня");
        assertDateTime(nowAt(2027, 6, 5), "5 июня 2027 года");
    }

    @Test
    public void times() {
        assertDateTime(at(5, 10, 19, 30), "в 7 часов 30 минут вечера");
        assertDateTime(at(5, 10, 15, 0), "в три часа дня");
        assertDateTime(at(5, 10, 19, 30), "в 19:30");
        assertDateTime(at(5, 12, 7, 0), "послезавтра в 7 утра");
        assertDateTime(at(5, 9, 0, 0), "вчера в полночь");
    }

    @Test
    public void notDateTimes() {
        assertNull(pf.extractDateTime("привет как дела").now(NOW).parseFirst());
    }
}
