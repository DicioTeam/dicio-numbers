package org.dicio.numbers.lang.ru;

import static org.dicio.numbers.test.TestUtils.F;
import static org.dicio.numbers.test.TestUtils.T;

import org.dicio.numbers.formatter.Formatter;
import org.dicio.numbers.test.NiceDurationTestBase;
import org.junit.Test;

public class NiceDurationTest extends NiceDurationTestBase {

    @Override
    public Formatter buildNumberFormatter() {
        return new RussianFormatter();
    }

    @Test
    public void zero() {
        assertDuration("ноль секунд", T, 0, 0, 0, 0);
        assertDuration("0:00", F, 0, 0, 0, 0);
    }

    @Test
    public void speechOne() {
        assertDuration("одна секунда", T, 0, 0, 0, 1);
        assertDuration("одна минута", T, 0, 0, 1, 0);
        assertDuration("один час", T, 0, 1, 0, 0);
        assertDuration("один день", T, 1, 0, 0, 0);
    }

    @Test
    public void speechPluralForms() {
        assertDuration("две минуты", T, 0, 0, 2, 0);
        assertDuration("пять минут", T, 0, 0, 5, 0);
        assertDuration("одиннадцать минут", T, 0, 0, 11, 0);
        assertDuration("двадцать одна минута", T, 0, 0, 21, 0);
        assertDuration("двадцать два часа", T, 0, 22, 0, 0);
        assertDuration("сорок четыре секунды", T, 0, 0, 0, 44);
        assertDuration("сто дней", T, 100, 0, 0, 0);
    }

    @Test
    public void speechMixed() {
        assertDuration("один день два часа три минуты четыре секунды", T, 1, 2, 3, 4);
        assertDuration("пять часов одна секунда", T, 0, 5, 0, 1);
    }

    @Test
    public void noSpeech() {
        assertDuration("1d 2:03:04", F, 1, 2, 3, 4);
        assertDuration("7:00", F, 0, 0, 7, 0);
    }
}
