package org.dicio.numbers.lang.fr;

import static org.dicio.numbers.test.TestUtils.t;
import static org.dicio.numbers.util.NumberExtractorUtils.signBeforeNumber;
import static java.time.temporal.ChronoUnit.MONTHS;

import org.dicio.numbers.parser.lexer.TokenStream;
import org.dicio.numbers.test.DateTimeExtractorUtilsTestBase;
import org.dicio.numbers.util.DateTimeExtractorUtils;
import org.dicio.numbers.util.NumberExtractorUtils;
import org.junit.Test;

import java.time.LocalDateTime;

public class DateTimeExtractorUtilsTest extends DateTimeExtractorUtilsTestBase {

    // Tuesday the 10th of May, 2022, 19:38:36
    private static final LocalDateTime NOW = LocalDateTime.of(2022, 5, 10, 19, 38, 36, 295834726);

    @Override
    public String configFolder() {
        return "config/fr-fr";
    }

    @Override
    public DateTimeExtractorUtils build(final TokenStream ts) {
        final FrenchNumberExtractor numberExtractor = new FrenchNumberExtractor(ts);
        return new DateTimeExtractorUtils(ts, NOW, (fromInclusive, toInclusive) ->
            NumberExtractorUtils.extractOneIntegerInRange(ts, fromInclusive, toInclusive,
                    () -> signBeforeNumber(ts, () -> numberExtractor.numberInteger(false)))
        );
    }

    @Test
    public void testRelativeMonthDuration() {
        assertRelativeMonthDuration("septembre prochain",   t(4, MONTHS),   2);
        assertRelativeMonthDuration("avril prochain",       t(11, MONTHS),  2);
        assertRelativeMonthDuration("avril dernier",        t(-1, MONTHS),  2);
        assertRelativeMonthDuration("en janvier",           t(8, MONTHS),   2);
    }

    @Test
    public void testRelativeMonthDurationNull() {
        assertRelativeMonthDurationNull("bonjour comment va");
        assertRelativeMonthDurationNull("octobre");
        assertRelativeMonthDurationNull("dans deux mois");
    }

    @Test
    public void testRelativeDayOfWeekDuration() {
        assertRelativeDayOfWeekDuration("jeudi prochain",   3,   2);
        assertRelativeDayOfWeekDuration("jeudi dernier",    -4,  2);
        assertRelativeDayOfWeekDuration("lundi prochain",   -6,  2);
    }
}
