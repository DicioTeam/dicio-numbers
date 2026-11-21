package org.dicio.numbers.lang.ru;

import static org.dicio.numbers.test.TestUtils.DAY;
import static org.dicio.numbers.test.TestUtils.HOUR;
import static org.dicio.numbers.test.TestUtils.MINUTE;
import static org.dicio.numbers.test.TestUtils.t;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.dicio.numbers.ParserFormatter;
import org.junit.BeforeClass;
import org.junit.Test;

public class ExtractDurationTest {

    private static ParserFormatter pf;

    @BeforeClass
    public static void setup() {
        pf = new ParserFormatter(null, new RussianParser());
    }

    private static void assertDuration(final java.time.Duration expected, final String s) {
        assertEquals("wrong duration for \"" + s + "\"", expected,
                pf.extractDuration(s).parseFirst().toJavaDuration());
    }

    @Test
    public void timer() {
        assertDuration(t(5 * MINUTE), "поставь таймер на пять минут");
        assertDuration(t(7 * MINUTE), "таймер на семь минут");
        assertDuration(t(7 * MINUTE), "таймер на 7 минут");
        assertDuration(t(MINUTE), "на минуту");
        assertDuration(t(10), "на десять секунд");
        assertDuration(t(21 * MINUTE), "двадцать одну минуту");
        assertDuration(t(22 * MINUTE), "двадцать две минуты");
    }

    @Test
    public void fractions() {
        assertDuration(t(90 * MINUTE), "полтора часа");
        assertDuration(t(90), "полторы минуты");
        assertDuration(t(150 * MINUTE), "два с половиной часа");
        assertDuration(t(30 * MINUTE), "полчаса");
        assertDuration(t(30), "полминуты");
        assertDuration(t(15 * MINUTE), "четверть часа");
    }

    @Test
    public void combined() {
        assertDuration(t(2 * HOUR + 30 * MINUTE), "через 2 часа 30 минут");
        assertDuration(t(DAY + 3 * HOUR), "один день и три часа");
        assertDuration(t(HOUR + 5 * MINUTE + 3), "1 ч 5 мин 3 с");
        assertDuration(t(2 * DAY), "2 суток");
    }

    @Test
    public void notDurations() {
        assertNull(pf.extractDuration("сколько времени").parseFirst());
        assertNull(pf.extractDuration("двадцать человек").parseFirst());
        assertNull(pf.extractDuration("с пятью друзьями").parseFirst());
    }
}
