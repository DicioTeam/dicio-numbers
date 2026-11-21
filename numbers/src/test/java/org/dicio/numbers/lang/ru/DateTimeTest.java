package org.dicio.numbers.lang.ru;

import static org.junit.Assert.assertEquals;

import org.dicio.numbers.formatter.Formatter;
import org.dicio.numbers.test.DateTimeTestBase;
import org.junit.Test;

import java.time.LocalDate;

public class DateTimeTest extends DateTimeTestBase {

    @Override
    public String configFolder() {
        return "config/ru-ru";
    }

    @Override
    public Formatter buildNumberFormatter() {
        return new RussianFormatter();
    }

    @Test
    public void testNiceDateParameters() {
        // just check that the ParserFormatter functions do their job
        assertEquals("среда, двадцать восьмое апреля две тысячи двадцать первого года",
                pf.niceDate(LocalDate.of(2021, 4, 28)).get());
        assertEquals("тысяча девятьсот восемьдесят четвёртый",
                pf.niceYear(LocalDate.of(1984, 4, 28)).get());
    }
}
