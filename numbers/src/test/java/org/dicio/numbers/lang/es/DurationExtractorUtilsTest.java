package org.dicio.numbers.lang.es;

import static org.dicio.numbers.test.TestUtils.DAY;
import static org.dicio.numbers.test.TestUtils.F;
import static org.dicio.numbers.test.TestUtils.HOUR;
import static org.dicio.numbers.test.TestUtils.MICROS;
import static org.dicio.numbers.test.TestUtils.MILLIS;
import static org.dicio.numbers.test.TestUtils.MINUTE;
import static org.dicio.numbers.test.TestUtils.MONTH;
import static org.dicio.numbers.test.TestUtils.T;
import static org.dicio.numbers.test.TestUtils.WEEK;
import static org.dicio.numbers.test.TestUtils.YEAR;
import static org.dicio.numbers.test.TestUtils.t;
import static org.junit.Assert.assertTrue;

import org.dicio.numbers.ParserFormatter;
import org.dicio.numbers.parser.lexer.TokenStream;
import org.dicio.numbers.test.DurationExtractorUtilsTestBase;
import org.dicio.numbers.unit.Duration;
import org.dicio.numbers.util.DurationExtractorUtils;
import org.junit.Test;


public class DurationExtractorUtilsTest extends DurationExtractorUtilsTestBase {

    @Override
    public String configFolder() {
        return "config/es-es";
    }

    @Override
    public Duration extractDuration(final TokenStream ts, final boolean shortScale) {
        // NOTE (ES): The SpanishNumberExtractor constructor does not take a shortScale parameter,
        // as Spanish exclusively uses the long scale for numbers.
        final SpanishNumberExtractor numberExtractor = new SpanishNumberExtractor(ts);
        return new DurationExtractorUtils(ts, numberExtractor::numberNoOrdinal).duration();
    }


    @Test
    public void testDurationNumberAndUnit() {
        assertDuration("mil millones de nanosegundos", T, t(1_000_000_000L)); // 10^9 nanos = 1 second
        assertDuration("mil setecientos veintiocho μs", T, t(0, 1728 * MICROS));
        assertDuration("cien milisegundos",             T, t(0, 100 * MILLIS));
        assertDuration("18s",                           F, t(18));
        assertDuration("un seg",                        F, t(1));
        assertDuration("cincuenta y nueve minutos",     T, t(59 * MINUTE));
        assertDuration("veintitrés horas",              F, t(23 * HOUR));
        assertDuration("media hora",                    T, t(HOUR / 2));
        assertDuration("uno coma dos días",             T, t(1.2 * DAY));
        assertDuration("medio día",                     F, t(DAY / 2));
        assertDuration("diez semanas",                  F, t(10 * WEEK));
        assertDuration("6 meses",                       T, t(6 * MONTH));
        assertDuration("tres mil millones de años",     T, t(3e9 * YEAR));
        assertDuration("quince décadas",                T, t(150 * YEAR));
        // NOTE (ES): Spanish uses long scale, so a billionth is 10^-12
        assertDuration("un siglo billonésimo",          T, t(1e-12 * 100 * YEAR));
        assertDuration("1 milenio",                     F, t(1000 * YEAR));
        assertNoDuration("cuarenta y tres milenios cuatro", T);
        assertNoDuration("y diez semanas y", F);
        assertNoDuration("cien pruebas", F);
        assertNoDuration("coma treinta y cuatro gramos", T);
    }

    @Test
    public void testDurationOnlyUnit() {
        assertDuration("hora minuto milenio",                  T, t(1000 * YEAR + HOUR + MINUTE));
        assertDuration("milisegundo y segundo, microsegundo",  F, t(1, MILLIS + MICROS));
        assertDuration("segundos segundo s",                   T, t(2));
        assertDuration("minuto horas años",                    F, t(MINUTE + HOUR + YEAR)); // Corrected to include year
        assertNoDuration("hola milisegundo", F);
        assertNoDuration("está bien", T);
        assertNoDuration("ns μs ms s m h d sem mes a", F);
    }

    @Test
    public void testDurationOf() {
        assertDuration("dos décimas de segundo", F, t(0, 200 * MILLIS));
        assertDuration("un par de horas",        F, t(2 * HOUR));
        assertNoDuration("muchos segundos", F);
        assertNoDuration("decenas de líneas de prueba", T);
        assertNoDuration("hola dos cientos de hola", F);
        assertNoDuration("hola de semana", F);
    }

    @Test
    public void testMultipleDurationGroups() {
        assertDuration("veinte minutos y treinta y seis segundos porque", T, t(20 * MINUTE + 36));
        assertDuration("siete días, veintiuna horas y doce minutos para llegar", F, t(7 * DAY + 21 * HOUR + 12 * MINUTE));
        assertDuration("minuto, segundo y milisegundo, microsegundo y nanosegundo", T, t(MINUTE + 1, MILLIS + MICROS + 1));
        assertDuration("5 ns ns", F, t(0, 5+1)); // 5 nanos + 1 nano
        assertNoDuration("ms 5 ns ns", F); // Number cannot be in the middle
    }

    @Test(timeout = 4000)
    public void testPerformanceWithFormatter() {
        final java.time.Duration[] alternatives = {
                t(1), t(5 * MINUTE), t(2 * HOUR), t(16 * DAY), t(WEEK), t(3 * MONTH), t(5 * YEAR),
                t(1e8 * YEAR), t(17 * WEEK), t(45)
        };

        final ParserFormatter npf = new ParserFormatter(new SpanishFormatter(), null);
        for (int i = 0; i < (1 << alternatives.length); ++i) {
            java.time.Duration durationToTest = java.time.Duration.ZERO;
            for (int j = 0; j < alternatives.length; ++j) {
                if ((i & (1 << j)) != 0) {
                    durationToTest = durationToTest.plus(alternatives[j]);
                }
            }

            // The Spanish formatter correctly handles the long scale numbers.
            final String formatted = npf.niceDuration(new Duration(durationToTest)).get();
            final TokenStream ts = new TokenStream(tokenizer.tokenize(formatted));
            assertDuration(formatted, ts, T, durationToTest);
            assertTrue(ts.finished());
        }
    }
}