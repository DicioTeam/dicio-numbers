package org.dicio.numbers.lang.de;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.dicio.numbers.ParserFormatter;
import org.dicio.numbers.parser.lexer.TokenStream;
import org.dicio.numbers.test.WithTokenizerTestBase;
import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.function.Function;

public class ExtractDateTimeTest extends WithTokenizerTestBase {

    // Tuesday the 10th of May, 2022, 19:38:36
    private static final LocalDateTime NOW = LocalDateTime.of(2022, 5, 10, 19, 38, 36, 295834726);

    @Override
    public String configFolder() {
        return "config/de-de";
    }


    private <T> void assertFunction(final String s,
                                    final T expected,
                                    final int finalTokenStreamPosition,
                                    final Function<GermanDateTimeExtractor, T> function) {
        final TokenStream ts = new TokenStream(tokenizer.tokenize(s));
        assertEquals("wrong result for string \"" + s + "\"",
                expected, function.apply(new GermanDateTimeExtractor(ts, NOW)));
        assertEquals("wrong final token position for string \"" + s + "\"",
                finalTokenStreamPosition, ts.position);
    }

    private <T> void assertFunctionNull(final String s,
                                        final Function<GermanDateTimeExtractor, T> function) {
        assertFunction(s, null, 0, function);
    }

    private void assertTime(final String s, final int hour, final int minute, final int finalTokenStreamPosition) {
        assertFunction(s, LocalTime.of(hour, minute), finalTokenStreamPosition,
                GermanDateTimeExtractor::timeWithMomentOfDay);
    }

    private void assertDate(final String s, final LocalDate expected, final int finalTokenStreamPosition) {
        assertFunction(s, expected, finalTokenStreamPosition, GermanDateTimeExtractor::date);
    }

    private void assertDateTime(final String s, final LocalDateTime expected, final int finalTokenStreamPosition) {
        assertFunction(s, expected, finalTokenStreamPosition, GermanDateTimeExtractor::dateTime);
    }

    private static LocalDateTime at(final int month, final int day, final int hour, final int minute) {
        return LocalDateTime.of(2022, month, day, hour, minute);
    }

    private static LocalDateTime atNowTime(final int year, final int month, final int day) {
        return LocalDate.of(year, month, day).atTime(NOW.toLocalTime());
    }


    @Test
    public void testRelativeDays() {
        assertFunction("morgen",       1,  1, GermanDateTimeExtractor::relativeTomorrow);
        assertFunction("übermorgen",   2,  2, GermanDateTimeExtractor::relativeTomorrow);
        assertFunction("gestern",      -1, 1, GermanDateTimeExtractor::relativeYesterday);
        assertFunction("vorgestern",   -2, 2, GermanDateTimeExtractor::relativeYesterday);
        assertFunctionNull("heute", GermanDateTimeExtractor::relativeTomorrow);
        assertFunctionNull("vor",   GermanDateTimeExtractor::relativeYesterday);
    }

    @Test
    public void testClockTime() {
        assertTime("um drei",                 3,  0,  2);
        assertTime("fünfzehn Uhr dreißig",    15, 30, 3);
        assertTime("um fünfzehn Uhr",         15, 0,  3);
        assertTime("15:30",                   15, 30, 2);
        assertTime("15.30 Uhr",               15, 30, 3);
        assertTime("ein Uhr fünf",            1,  5,  3);
        assertTime("um Mitternacht",          0,  0,  2);
        assertTime("Mittag",                  12, 0,  1);
    }

    @Test
    public void testRelativeTime() {
        assertTime("halb vier",               3,  30, 2);
        assertTime("um halb zwölf",           11, 30, 3);
        assertTime("viertel nach drei",       3,  15, 3);
        assertTime("viertel vor vier",        3,  45, 3);
        assertTime("dreiviertel vier",        3,  45, 3);
        assertTime("viertel vier",            3,  15, 2);
        assertTime("zehn vor acht",           7,  50, 3);
        assertTime("zwanzig nach neun",       9,  20, 3);
        assertTime("fünf nach halb drei",     2,  35, 4);
        assertTime("fünf vor halb drei",      2,  25, 4);
    }

    @Test
    public void testMomentOfDay() {
        assertTime("abends",                  19, 0,  1);
        assertTime("um acht Uhr abends",      20, 0,  4);
        assertTime("morgens um sieben",       7,  0,  3);
        assertTime("halb vier nachmittags",   15, 30, 3);
        assertTime("um drei nachts",          3,  0,  3);
        assertTime("zehn vor acht abends",    19, 50, 4);
    }

    @Test
    public void testDate() {
        assertDate("dritten Oktober",               LocalDate.of(2022, 10, 3), 2);
        assertDate("3. Oktober",                    LocalDate.of(2022, 10, 3), 3);
        assertDate("3.10.2026",                     LocalDate.of(2026, 10, 3), 5);
        assertDate("dritten zehnten",               LocalDate.of(2022, 10, 3), 2);
        assertDate("Montag, der dritte Oktober",    LocalDate.of(2022, 10, 3), 5);
        assertDate("einunddreißigsten Dezember 1999", LocalDate.of(1999, 12, 31), 5);
        assertDate("Freitag",                       LocalDate.of(2022, 5, 13), 1);
        assertDate("Dienstag",                      LocalDate.of(2022, 5, 10), 1);
        assertDate("Oktober",                       LocalDate.of(2022, 10, 1), 1);
        assertFunctionNull("zehn vor acht", GermanDateTimeExtractor::date);
        assertFunctionNull("zehn",          GermanDateTimeExtractor::date);
    }

    @Test
    public void testDateTime() {
        assertDateTime("morgen um acht",          at(5, 11, 8, 0),   3);
        assertDateTime("übermorgen",              atNowTime(2022, 5, 12), 2);
        assertDateTime("vorgestern",              atNowTime(2022, 5, 8), 2);
        assertDateTime("heute Abend",             at(5, 10, 19, 0),  2);
        assertDateTime("heute morgen",            at(5, 10, 9, 0),   2);
        assertDateTime("morgen früh um sieben",   at(5, 11, 7, 0),   4);
        assertDateTime("um acht morgen",          at(5, 11, 8, 0),   3);
        assertDateTime("Freitag morgen",          at(5, 13, 9, 0),   2);
        assertDateTime("zehn vor acht",           at(5, 10, 7, 50),  3);
        assertDateTime("nächsten Montag",         atNowTime(2022, 5, 16), 2);
        assertDateTime("letzten Freitag",         atNowTime(2022, 5, 6), 2);
        assertDateTime("in zwei Stunden",         NOW.plusHours(2),  3);
        assertDateTime("in drei Tagen um acht",   at(5, 13, 8, 0),   5);
        assertDateTime("vor zwei Wochen",         atNowTime(2022, 4, 26), 3);
        assertDateTime("nächste Woche",           atNowTime(2022, 5, 17), 2);
        assertDateTime("dritten Oktober um halb drei nachmittags", at(10, 3, 14, 30), 6);
        assertFunctionNull("hallo wie geht es", GermanDateTimeExtractor::dateTime);
    }

    @Test
    public void testNumberParserExtractDateTime() {
        final ParserFormatter npf = new ParserFormatter(null, new GermanParser());
        assertNull(npf.extractDateTime("hallo wie geht es dir").parseFirst());
        assertEquals(at(10, 3, 14, 30),
                npf.extractDateTime("am dritten Oktober um halb drei nachmittags").now(NOW).parseFirst());
        assertEquals(at(5, 11, 15, 0),
                npf.extractDateTime("erinnere mich morgen um fünfzehn Uhr").now(NOW).parseFirst());
    }
}
