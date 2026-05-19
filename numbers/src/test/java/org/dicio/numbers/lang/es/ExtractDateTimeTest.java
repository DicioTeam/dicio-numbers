package org.dicio.numbers.lang.es;

import static org.dicio.numbers.test.TestUtils.F;
import static org.dicio.numbers.test.TestUtils.T;
import static org.dicio.numbers.test.TestUtils.t;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.MONTHS;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.time.temporal.ChronoUnit.WEEKS;
import static java.time.temporal.ChronoUnit.YEARS;

import org.dicio.numbers.ParserFormatter;
import org.dicio.numbers.lang.es.SpanishParser;
import org.dicio.numbers.parser.lexer.TokenStream;
import org.dicio.numbers.test.WithTokenizerTestBase;
import org.dicio.numbers.unit.Duration;
import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.function.Function;

public class ExtractDateTimeTest extends WithTokenizerTestBase {

    // NOTE (ES): Reference date is a Sunday.
    // Sunday, 5th of February, 2023, 9:41:12
    private static final LocalDateTime NOW = LocalDateTime.of(2023, 2, 5, 9, 41, 12, 759274821);

    @Override
    public String configFolder() {
        return "config/es-es";
    }

    // --- Helper assertion methods ---

    private <T> void assertFunction(final String s,
                                    final boolean preferMonthBeforeDay,
                                    final T expectedResult,
                                    int finalTokenStreamPosition,
                                    final Function<SpanishDateTimeExtractor, T> function) {
        final TokenStream ts = new TokenStream(tokenizer.tokenize(s));
        assertEquals("Wrong result for string \"" + s + "\"",
                expectedResult, function.apply(new SpanishDateTimeExtractor(ts, preferMonthBeforeDay, NOW)));
        assertEquals("Wrong final token position for string \"" + s + "\"",
                finalTokenStreamPosition, ts.position);
    }

    private <T> void assertFunctionNull(final String s,
                                        final boolean preferMonthBeforeDay,
                                        final Function<SpanishDateTimeExtractor, T> numberFunction) {
        assertFunction(s, preferMonthBeforeDay, null, 0, numberFunction);
    }

    // Overloads for cleaner test code
    private void assertRelativeDuration(final String s, final Duration expectedDuration, int finalTokenStreamPosition) {
        assertFunction(s, false, expectedDuration, finalTokenStreamPosition, SpanishDateTimeExtractor::relativeDuration);
    }
    private void assertRelativeDurationNull(final String s) { assertFunctionNull(s, false, SpanishDateTimeExtractor::relativeDuration); }
    private void assertRelativeTomorrow(final String s, final int expectedDuration, int finalTokenStreamPosition) { assertFunction(s, false, expectedDuration, finalTokenStreamPosition, SpanishDateTimeExtractor::relativeTomorrow); }
    private void assertRelativeTomorrowNull(final String s) { assertFunctionNull(s, false, SpanishDateTimeExtractor::relativeTomorrow); }
    private void assertRelativeYesterday(final String s, final int expectedDuration, int finalTokenStreamPosition) { assertFunction(s, false, expectedDuration, finalTokenStreamPosition, SpanishDateTimeExtractor::relativeYesterday); }
    private void assertRelativeYesterdayNull(final String s) { assertFunctionNull(s, false, SpanishDateTimeExtractor::relativeYesterday); }
    private void assertHour(final String s, final int expected, int finalTokenStreamPosition) { assertFunction(s, false, expected, finalTokenStreamPosition, SpanishDateTimeExtractor::hour); }
    private void assertHourNull(final String s) { assertFunctionNull(s, false, SpanishDateTimeExtractor::hour); }
    private void assertMomentOfDay(final String s, final int expected, int finalTokenStreamPosition) { assertFunction(s, false, expected, finalTokenStreamPosition, SpanishDateTimeExtractor::momentOfDay); }
    private void assertMomentOfDayNull(final String s) { assertFunctionNull(s, false, SpanishDateTimeExtractor::momentOfDay); }
    private void assertNoonMidnightLike(final String s, final int expected, int finalTokenStreamPosition) { assertFunction(s, false, expected, finalTokenStreamPosition, SpanishDateTimeExtractor::noonMidnightLike); }
    private void assertNoonMidnightLikeNull(final String s) { assertFunctionNull(s, false, SpanishDateTimeExtractor::noonMidnightLike); }
    private void assertDate(final String s, final boolean preferMonthBeforeDay, final LocalDate expected, int finalTokenStreamPosition) { assertFunction(s, preferMonthBeforeDay, expected, finalTokenStreamPosition, SpanishDateTimeExtractor::date); }
    private void assertDateNull(final String s) { assertFunctionNull(s, false, SpanishDateTimeExtractor::date); }
    private void assertTime(final String s, final LocalTime expected, int finalTokenStreamPosition) { assertFunction(s, false, expected, finalTokenStreamPosition, SpanishDateTimeExtractor::time); }
    private void assertTimeNull(final String s) { assertFunctionNull(s, false, SpanishDateTimeExtractor::time); }
    private void assertTimeWithAmpm(final String s, final LocalTime expected, int finalTokenStreamPosition) { assertFunction(s, false, expected, finalTokenStreamPosition, SpanishDateTimeExtractor::timeWithAmpm); }
    private void assertTimeWithAmpmNull(final String s) { assertFunctionNull(s, false, SpanishDateTimeExtractor::timeWithAmpm); }
    private void assertDateTime(final String s, final boolean preferMonthBeforeDay, final LocalDateTime expected, int finalTokenStreamPosition) { assertFunction(s, preferMonthBeforeDay, expected, finalTokenStreamPosition, SpanishDateTimeExtractor::dateTime); }
    private void assertDateTimeNull(final String s, final boolean preferMonthBeforeDay) { assertFunctionNull(s, preferMonthBeforeDay, SpanishDateTimeExtractor::dateTime); }

    // --- Spanish-specific tests ---

    @Test
    public void testRelativeDuration() {
        assertRelativeDuration("en dos semanas llegaré",       t(2, WEEKS), 3);
        assertRelativeDuration("hace cuatro meses",            t(-4, MONTHS), 3);
        assertRelativeDuration("un segundo después se cayó",   t(1, SECONDS), 3);
        assertRelativeDuration("dentro de un par de décadas",  t(20, YEARS), 6);
        assertRelativeDuration("nueve días antes",             t(-9, DAYS), 3);
        assertRelativeDuration("setenta años pasados",         t(-70, YEARS), 3);
        assertRelativeDuration("tres meses y dos días después",t(3, MONTHS).plus(t(2, DAYS)), 6);
    }

    @Test
    public void testRelativeDurationNull() {
        assertRelativeDurationNull("hola cómo estás");
        assertRelativeDurationNull("cuatro semestres"); // "semestre" is not a defined duration word
        assertRelativeDurationNull("sabes que en una semana"); // duration must be at the start
        assertRelativeDurationNull("y pasaron dos meses"); // same
        assertRelativeDurationNull("el día anterior"); // not a calculable duration
    }

    @Test
    public void testRelativeTomorrow() {
        assertRelativeTomorrow("mañana iremos",      1, 1);
        assertRelativeTomorrow("pasado mañana y",    2, 1); // "pasado mañana" is a single token
    }
    
    @Test
    public void testRelativeYesterday() {
        assertRelativeYesterday("ayer yo estuve", -1, 1);
        assertRelativeYesterday("anteayer prueba",-2, 1); // "anteayer" is a single word
    }

    @Test
    public void testHour() {
        assertHour("a las ocho y treinta y seis",  8, 3);
        assertHour("veintiuna y dos",              21, 1);
        assertHour("a la una y veintiséis",        1, 3);
        assertHour("las diecisiete el",            17, 2);
        assertHour("hora trece",                   13, 2);
    }
    
    @Test
    public void testNoonMidnightLike() {
        assertNoonMidnightLike("al mediodía", 12, 2);
        assertNoonMidnightLike("medianoche",   0, 1);
    }

    @Test
    public void testMomentOfDay() {
        assertMomentOfDay("a la medianoche",      0, 3);
        assertMomentOfDay("mediodía",             12, 1);
        assertMomentOfDay("esta tarde y",         15, 2);
        assertMomentOfDay("por la noche prueba",  21, 3);
        assertMomentOfDay("la cena",              20, 2);
    }

    @Test
    public void testDate() {
        // NOTE (ES): Default Spanish format is DD/MM/YYYY. preferMonthBeforeDay=T will test for MM/DD/YYYY.
        assertDate("04/09/4096", F, LocalDate.of(4096, 9, 4), 5);
        assertDate("04/09/4096", T, LocalDate.of(4096, 4, 9), 5);
        assertDate("13 4 2023", F, LocalDate.of(2023, 4, 13), 3);
        assertDate("seis de julio de mil novecientos noventa y cinco", T, LocalDate.of(1995, 7, 6), 9);
        assertDate("jueves 26 de mayo de 2022", T, LocalDate.of(2022, 5, 26), 6);
        assertDate("2 de enero del 2 a.C.", T, LocalDate.of(-1, 1, 2), 7); // 2 BC is year -1
        assertDate("doce de junio de dos mil doce a.C.", T, LocalDate.of(-2011, 6, 12), 9);
        assertDate("cuatrocientos setenta y seis d.C.", T, LocalDate.of(476, 2, 5), 6);
        assertDate("martes veintisiete", T, LocalDate.of(2023, 2, 28), 2); // NOW is Sun 5th, next Tue is 7th, so Tue 27th must be Feb 28th
        assertDate("lunes de noviembre", T, LocalDate.of(2023, 11, 6), 3);
    }

    @Test
    public void testTime() {
        assertTime("13:28:33 prueba",                 LocalTime.of(13, 28, 33), 3);
        assertTime("mediodía y cuarto",               LocalTime.of(12, 15, 0), 3);
        assertTime("a las catorce",                   LocalTime.of(14, 0,  0), 3);
        assertTime("medianoche y doce",               LocalTime.of(0, 12, 0),  3);
        assertTime("las veintitrés y cincuenta y un minutos", LocalTime.of(23, 51, 0), 7);
        assertTime("las cinco y media",               LocalTime.of(5, 30, 0), 4);
        assertTime("las seis menos cuarto",           LocalTime.of(5, 45, 0), 4);
    }

    @Test
    public void testTimeWithAmpm() {
        assertTimeWithAmpm("11:28:33 pm test",                LocalTime.of(23, 28, 33), 4);
        assertTimeWithAmpm("a las dos de la mañana",          LocalTime.of(2,  0,  0),  6);
        assertTimeWithAmpm("tres y treinta y ocho de la tarde", LocalTime.of(15, 38, 0), 8);
        assertTimeWithAmpm("noche",                           LocalTime.of(21, 0,  0),  1);
        assertTimeWithAmpm("tarde a las cuatro y tres",       LocalTime.of(16, 3,  0),  6);
        assertTimeWithAmpm("12 am",                           LocalTime.of(0,  0,  0),  2); // 12 AM is midnight
        assertTimeWithAmpm("12 pm",                           LocalTime.of(12, 0,  0),  2); // 12 PM is noon
    }

    @Test
    public void testDateTime() {
        // NOTE (ES): All expected values are calculated from NOW (Sun, Feb 5, 2023 09:41:12).
        assertDateTime("mañana a las 12:45", F, LocalDateTime.of(2023, 2,  6, 12, 45, 0), 5);
        assertDateTime("26/12/2003 19:18:59", F, LocalDateTime.of(2003, 12, 26, 19, 18, 59), 4);
        assertDateTime("19:18:59 26/12/2003 test", F, LocalDateTime.of(2003, 12, 26, 19, 18, 59), 4);
        assertDateTime("05/07/2003 1:2:3", F, LocalDateTime.of(2003, 7, 5, 1, 2, 3), 4); // Standard Spanish DD/MM
        assertDateTime("05/07/2003 1:2:3", T, LocalDateTime.of(2003, 5, 7, 1, 2, 3), 4); // preferMonthBeforeDay MM/DD
        assertDateTime("próximo viernes a las veintidós en punto", F, LocalDateTime.of(2023, 2, 10, 22, 0, 0), 7);
        assertDateTime("ayer por la tarde a las cinco menos cuarto", F, LocalDateTime.of(2023, 2, 4, 16, 45, 0), 9);
        assertDateTime("dentro de tres días por la noche a las once", F, LocalDateTime.of(2023, 2, 8, 23, 0, 0), 9);
        assertDateTime("pasado mañana por la mañana", F, LocalDateTime.of(2023, 2, 7, 9, 0, 0), 4);
        assertDateTime("domingo a las 2:45 p.m.", F, LocalDateTime.of(2023, 2, 5, 14, 45, 0), 6);
        assertDateTime("hace dos días al atardecer", F, LocalDateTime.of(2023, 2, 3, 18, 0, 0), 5);
        assertDateTime("siete de noviembre de 193 a.C.", T, LocalDateTime.of(-192, 11, 7, 9, 41, 12), 8); // 193 BC is year -192
    }

    @Test
    public void testDateTimeNull() {
        assertDateTimeNull("hola cómo estás", F);
        assertDateTimeNull("prueba veintiuno de enero después de cenar", F);
        assertDateTimeNull("menos un milisegundo", F);
    }

    @Test
    public void testNumberParserExtractDateTime() {
        // NOTE (ES): This tests the top-level ParserFormatter class.
        final ParserFormatter npf = new ParserFormatter(null, new SpanishParser());
        assertNull(npf.extractDateTime("hola cómo estás").getFirst());
        assertEquals(NOW.minusDays(30).withHour(14).withMinute(39).withSecond(0).withNano(0),
                npf.extractDateTime("2:39 p.m. hace treinta días").now(NOW).getFirst());
        assertEquals(NOW.plusMinutes(3).plusSeconds(46),
                npf.extractDateTime("dentro de tres minutos y cuarenta y seis segundos").now(NOW).getFirst());
    }
}