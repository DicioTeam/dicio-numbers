package org.dicio.numbers.lang.fr;

import static org.dicio.numbers.test.TestUtils.DAY;
import static org.dicio.numbers.test.TestUtils.HOUR;
import static org.dicio.numbers.test.TestUtils.MILLIS;
import static org.dicio.numbers.test.TestUtils.MINUTE;
import static org.dicio.numbers.test.TestUtils.MONTH;
import static org.dicio.numbers.test.TestUtils.WEEK;
import static org.dicio.numbers.test.TestUtils.YEAR;
import static org.dicio.numbers.test.TestUtils.t;

import org.dicio.numbers.parser.lexer.TokenStream;
import org.dicio.numbers.test.DurationExtractorUtilsTestBase;
import org.dicio.numbers.unit.Duration;
import org.dicio.numbers.util.DurationExtractorUtils;
import org.junit.Test;

public class DurationExtractorUtilsTest extends DurationExtractorUtilsTestBase {

    @Override
    public String configFolder() {
        return "config/fr-fr";
    }

    @Override
    public Duration extractDuration(final TokenStream ts, final boolean shortScale) {
        final FrenchNumberExtractor numberExtractor = new FrenchNumberExtractor(ts);
        return new DurationExtractorUtils(ts, numberExtractor::numberNoOrdinal).duration();
    }

    private void assertDuration(final String s, final java.time.Duration duration) {
        assertDuration(s, true, duration);
    }

    private void assertNoDuration(final String s) {
        assertNoDuration(s, true);
    }

    @Test
    public void testDurationNumberAndUnit() {
        assertDuration("18s",                     t(18));
        assertDuration("une seconde",             t(1));
        assertDuration("cinquante-neuf minutes",  t(59 * MINUTE));
        assertDuration("vingt-trois heures",      t(23 * HOUR));
        assertDuration("cinq jours",              t(5 * DAY));
        assertDuration("dix semaines",            t(10 * WEEK));
        assertDuration("six mois",                t(6 * MONTH));
        assertDuration("trois ans",               t(3 * YEAR));
        assertDuration("cinq millisecondes",      t(0, 5 * MILLIS));
    }

    @Test
    public void testNoDuration() {
        assertNoDuration("bonjour");
        assertNoDuration("");
        assertNoDuration("mois");
    }
}
