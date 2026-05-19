package org.dicio.numbers.lang.es;

import org.dicio.numbers.formatter.Formatter;
import org.dicio.numbers.test.DateTimeTestBase;
import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.Assert.assertEquals;

public class DateTimeTest extends DateTimeTestBase {

    @Override
    public String configFolder() {
        return "config/es-es";
    }

    @Override
    public Formatter buildNumberFormatter() {
        return new SpanishFormatter();
    }

    @Test
    public void testNiceDate() {
        // NOTE (ES): Test that the Formatter correctly generates full dates in Spanish.
        // The expected format is "{weekday}, {day} de {month} de {year}".
        assertEquals("miércoles, veintiocho de abril de dos mil veintiuno",
                pf.niceDate(LocalDate.of(2021, 4, 28)).get());
        
        // Test for a BC date, ensuring the correct output.
        assertEquals("domingo, trece de agosto de ochenta y cuatro a.C.",
                pf.niceDate(LocalDate.of(-83, 8, 13)).get()); // -83 is 84 BC
    }

    @Test
    public void testNiceYear() {
        // NOTE (ES): Test that the Formatter correctly pronounces years in Spanish.
        assertEquals("mil novecientos ochenta y cuatro", pf.niceYear(LocalDate.of(1984, 4, 28)).get());
        assertEquals("ochocientos diez a.C.", pf.niceYear(LocalDate.of(-809, 8, 13)).get()); // -809 is 810 BC
    }

    @Test
    public void testNiceDateTime() {
        // NOTE (ES): Test that the Formatter correctly generates full date-time strings.
        // The expected format is "{date} a las {time}".
        assertEquals("miércoles, doce de septiembre de mil setecientos sesenta y cuatro al mediodía",
                pf.niceDateTime(LocalDateTime.of(1764, 9, 12, 12, 0)).get());
        
        // Test for a BC date with a specific time.
        assertEquals("jueves, tres de noviembre de trescientos veintiocho a.C. a las cinco y siete de la mañana",
                pf.niceDateTime(LocalDateTime.of(-327, 11, 3, 5, 7)).get());
    }
}