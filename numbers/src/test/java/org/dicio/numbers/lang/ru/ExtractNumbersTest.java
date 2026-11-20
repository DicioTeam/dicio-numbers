package org.dicio.numbers.lang.ru;

import static org.dicio.numbers.test.TestUtils.n;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.dicio.numbers.ParserFormatter;
import org.dicio.numbers.unit.Number;
import org.junit.BeforeClass;
import org.junit.Test;

public class ExtractNumbersTest {

    private static ParserFormatter pf;

    @BeforeClass
    public static void setup() {
        pf = new ParserFormatter(null, new RussianParser());
    }

    private static void assertNumber(final Number expected, final String s) {
        assertEquals("wrong number for \"" + s + "\"", expected, pf.extractNumber(s).parseFirst());
    }

    private static void assertOrdinal(final Number expected, final String s) {
        assertEquals("wrong ordinal for \"" + s + "\"", expected,
                pf.extractNumber(s).preferOrdinal(true).parseFirst());
    }

    @Test
    public void cardinals() {
        assertNumber(n(0), "ноль");
        assertNumber(n(7), "семь");
        assertNumber(n(17), "семнадцать");
        assertNumber(n(23), "двадцать три");
        assertNumber(n(125), "сто двадцать пять");
        assertNumber(n(200), "двести");
        assertNumber(n(340), "триста сорок");
        assertNumber(n(999), "девятьсот девяносто девять");
        assertNumber(n(1000), "тысяча");
        assertNumber(n(2027), "две тысячи двадцать семь");
        assertNumber(n(21000), "двадцать одна тысяча");
        assertNumber(n(3500000), "три миллиона пятьсот тысяч");
        assertNumber(n(5000000000L), "пять миллиардов");
    }

    @Test
    public void inflectedForms() {
        assertNumber(n(5), "с пятью друзьями");
        assertNumber(n(2), "между двумя домами");
        assertNumber(n(17), "до семнадцати лет");
        assertNumber(n(300), "около трёхсот");
        assertNumber(n(300), "около трехсот");
        assertNumber(n(1), "одну");
        assertNumber(n(40), "сорока");
    }

    @Test
    public void raw() {
        assertNumber(n(42), "42");
        assertNumber(n(1000000), "1 000 000");
        assertNumber(n(2.5), "2,5");
        assertNumber(n(2.5), "2.5");
        assertNumber(n(-5), "минус 5");
    }

    @Test
    public void fractions() {
        assertNumber(n(1.5), "полтора");
        assertNumber(n(1.5), "полторы");
        assertNumber(n(2.5), "два с половиной");
        assertNumber(n(0.5), "половина");
        assertNumber(n(0.25), "четверть");
        assertNumber(n(0.2), "одна пятая");
        assertNumber(n(2.0 / 3), "две третьих");
        assertNumber(n(3.14), "три целых четырнадцать сотых");
        assertNumber(n(3.5), "три запятая пять");
        assertNumber(n(0.2), "двадцать делить на сто");
        assertNumber(n(12), "дюжина");
        assertNumber(n(2), "пару");
    }

    @Test
    public void ordinals() {
        assertOrdinal(n(1, true), "первый");
        assertOrdinal(n(3, true), "третьего");
        assertOrdinal(n(5, true), "пятого");
        assertOrdinal(n(21, true), "двадцать первое");
        assertOrdinal(n(40, true), "сороковой");
        assertOrdinal(n(200, true), "двухсотый");
        assertOrdinal(n(1000, true), "тысячный");
        assertOrdinal(n(5, true), "5-го");
        assertOrdinal(n(5, true), "5-й");
        assertOrdinal(n(2027, true), "2027-го");
        assertNumber(n(5), "5 й");
    }

    @Test
    public void inSentences() {
        assertNumber(n(25), "сколько будет двадцать пять умножить на четыре");
        assertNumber(n(7), "поставь таймер на семь минут");
        assertNull(pf.extractNumber("привет как дела").parseFirst());
    }
}
