package org.dicio.numbers.lang.es;

import static org.dicio.numbers.test.TestUtils.t;
import static org.dicio.numbers.util.NumberExtractorUtils.signBeforeNumber;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.MONTHS;

import org.dicio.numbers.parser.lexer.TokenStream;
import org.dicio.numbers.test.DateTimeExtractorUtilsTestBase;
import org.dicio.numbers.util.DateTimeExtractorUtils;
import org.dicio.numbers.util.NumberExtractorUtils;
import org.junit.Test;

import java.time.LocalDateTime;

public class DateTimeExtractorUtilsTest extends DateTimeExtractorUtilsTestBase {

    // NOTE (ES): Reference date is a Saturday.
    // Saturday, 4th of February, 2023, 22:03:47
    private static final LocalDateTime NOW = LocalDateTime.of(2023, 2, 4, 22, 3, 47, 482175927);

    @Override
    public String configFolder() {
        return "config/es-es";
    }

    @Override
    public DateTimeExtractorUtils build(final TokenStream ts) {
        // Use the SpanishNumberExtractor. The boolean for shortScale is not needed in the Spanish constructor.
        final SpanishNumberExtractor numberExtractor = new SpanishNumberExtractor(ts);
        return new DateTimeExtractorUtils(ts, NOW, (fromInclusive, toInclusive) ->
            NumberExtractorUtils.extractOneIntegerInRange(ts, fromInclusive, toInclusive,
                    () -> signBeforeNumber(ts, () -> numberExtractor.numberInteger(false)))
        );
    }

    @Test
    public void testRelativeMonthDuration() {
        // NOTE (ES): All values recalculated from NOW (Feb 4th, 2023).
        assertRelativeMonthDuration("septiembre que viene",   t(7, MONTHS), 2); // Feb -> Sep is +7 months.
        assertRelativeMonthDuration("próximo abril y de",     t(2, MONTHS), 3); // Feb -> Apr is +2 months.
        assertRelativeMonthDuration("último abril y de",      t(-10, MONTHS), 3); // "last April" was in 2022, so it's -10 months from Feb 2023.
        assertRelativeMonthDuration("febrero que vendrá",     t(12, MONTHS), 2); // "upcoming February" is next year's.
        assertRelativeMonthDuration("febrero que pasó",       t(-12, MONTHS), 2); // "past February" is last year's.
        assertRelativeMonthDuration("enero pasado",           t(-1, MONTHS), 2);  // "last January" was in the current year.
    }

    @Test
    public void testRelativeMonthDurationNull() {
        assertRelativeMonthDurationNull("hola cómo estás");
        assertRelativeMonthDurationNull("en noviembre ocurrirá"); // "en" is not at the start of the duration indicator.
        assertRelativeMonthDurationNull("octubre"); // A month name alone is not a relative duration.
        assertRelativeMonthDurationNull("en dos octubres"); // Not a supported format for this util.
        assertRelativeMonthDurationNull("en dos meses");
    }

    @Test
    public void testRelativeToday() {
        assertRelativeToday("hoy");
        assertRelativeToday("hoy ahora mismo");
        assertRelativeToday("hoy prueba");
        assertRelativeToday("hoy y");
    }

    @Test
    public void testRelativeTodayNull() {
        assertRelativeTodayNull("hola cómo estás");
        assertRelativeTodayNull("el mismo hoy");
        assertRelativeTodayNull("el día de hoy");
        assertRelativeTodayNull("ayer");
        assertRelativeTodayNull("mañana");
    }

    @Test
    public void testRelativeDayOfWeekDuration() {
        // NOTE (ES): All values recalculated from NOW (Saturday, day 5).
        assertRelativeDayOfWeekDuration("próximo jueves",          5, 2); // Sat(5) -> next Thu(3) is 5 days.
        assertRelativeDayOfWeekDuration("el jueves pasado",        -2, 3); // Sat(5) -> last Thu(3) was 2 days ago.
        assertRelativeDayOfWeekDuration("hace dos domingos",       -13, 3); // Last Sun was yesterday (+1), the one before was 6 days ago (-6). Two Sundays ago is -13.
        assertRelativeDayOfWeekDuration("tres jueves siguientes",   19, 3); // Next Thu is +5, then +12, then +19.
        assertRelativeDayOfWeekDuration("cuatro martes antes",     -25, 3); // Last Tue was -4, then -11, -18, -25.
        assertRelativeDayOfWeekDuration("próximo sábado",           7, 2); // "upcoming Saturday" is next week's.
        assertRelativeDayOfWeekDuration("el sábado pasado",        -7, 3); // "saturday ago" was last week.
    }

    @Test
    public void testRelativeDayOfWeekDurationNull() {
        assertRelativeDayOfWeekDurationNull("hola cómo estás");
        assertRelativeDayOfWeekDurationNull("lunes"); // A day name alone is not a relative duration.
        assertRelativeDayOfWeekDurationNull("pasado lunes"); // "pasado" is a post-indicator.
        assertRelativeDayOfWeekDurationNull("dos viernes");
        assertRelativeDayOfWeekDurationNull("en dos días");
        assertRelativeDayOfWeekDurationNull("y en dos domingos");
        assertRelativeDayOfWeekDurationNull("un último lunes");
        assertRelativeDayOfWeekDurationNull("ayer y mañana");
    }

    @Test
    public void testMinute() {
        assertMinute("cero a b c",                0, 1);
        assertMinute("cincuenta y nueve horas",   59, 4); // "cincuenta y nueve" are 3 tokens + "horas"
        assertMinute("quince y",                  15, 2);
        assertMinute("veintiocho s",              28, 2);
        assertMinute("seis mins prueba",          6, 2);
        assertMinute("treinta y seis de min",     36, 5);
        assertMinute("44m de",                    44, 2);
    }

    @Test
    public void testMinuteNull() {
        assertMinuteNull("hola cómo estás");
        assertMinuteNull("sesenta minutos"); // 60 is an invalid minute value.
        assertMinuteNull("ciento veinte");
        assertMinuteNull("menos dieciséis");
        assertMinuteNull("12000 minutos");
        assertMinuteNull("y dos de");
    }

    @Test
    public void testSecond() {
        assertSecond("cero a b c",                0, 1);
        assertSecond("cincuenta y nueve horas",   59, 4);
        assertSecond("quince y",                  15, 2);
        assertSecond("veintiocho h",              28, 2);
        assertSecond("seis segs prueba",          6, 2);
        assertSecond("treinta y seis de seg",     36, 5);
        assertSecond("44s de",                    44, 2);
    }

    @Test
    public void testSecondNull() {
        assertSecondNull("hola cómo estás");
        assertSecondNull("sesenta segundos"); // 60 is an invalid second value.
        assertSecondNull("ciento veinte");
        assertSecondNull("menos dieciséis");
        assertSecondNull("12000 segundos");
        assertSecondNull("y dos de");
    }

    @Test
    public void testBcad() {
        assertBcad("a.C. prueba",     false, 3);
        assertBcad("d.C. y",          true, 3);
        assertBcad("dc prueba y",     true, 1);
        assertBcad("antes de Cristo", false, 3);
        assertBcad("después de Cristo", true, 3);
    }

    @Test
    public void testBcadNull() {
        assertBcadNull("a.m.");
        assertBcadNull("año Domini");
        assertBcadNull("y antes común");
        assertBcadNull("prueba c");
        assertBcadNull("m");
        assertBcadNull("c prueba");
    }

    @Test
    public void testAmpm() {
        assertAmpm("a.m. prueba",      false, 3);
        assertAmpm("p.m. y",          true, 3);
        assertAmpm("am y prueba",     false, 1);
        assertAmpm("post meridiano",  true, 2);
        assertAmpm("p y meridiem",    true, 3);
    }

    @Test
    public void testAmpmNull() {
        assertAmpmNull("d.C.");
        assertAmpmNull("ante prueba meridiem");
        assertAmpmNull("y post m");
        assertAmpmNull("prueba m");
        assertAmpmNull("c");
        assertAmpmNull("aym");
        assertAmpmNull("meridiano prueba");
    }

    @Test
    public void testMonthName() {
        assertMonthName("enero",    1);
        assertMonthName("dic e",    12);
        assertMonthName("septiembre", 9);
        assertMonthName("mar",      3);
    }

    @Test
    public void testMonthNameNull() {
        assertMonthNameNull("lunes");
        assertMonthNameNull("jaguar");
        assertMonthNameNull("hola feb");
        assertMonthNameNull("y dic de");
    }
}