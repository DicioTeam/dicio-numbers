package org.dicio.numbers.lang.fr;

import static org.dicio.numbers.test.TestUtils.F;
import static org.dicio.numbers.test.TestUtils.MINUTE;
import static org.dicio.numbers.test.TestUtils.T;
import static org.dicio.numbers.test.TestUtils.YEAR;
import static org.dicio.numbers.test.TestUtils.n;
import static org.dicio.numbers.test.TestUtils.t;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.dicio.numbers.ParserFormatter;
import org.dicio.numbers.parser.Parser;
import org.dicio.numbers.parser.param.ExtractNumberParams;
import org.dicio.numbers.parser.param.ParserParamsTestBase;
import org.dicio.numbers.unit.Number;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class ParserParamsTest extends ParserParamsTestBase {

    @Override
    protected Parser numberParser() {
        return new FrenchParser();
    }

    protected void assertNumberFirst(final String s, final Number expectedResult) {
        assertNumberFirst(s, true, F, F, expectedResult);
    }

    protected void assertNumberMixedWithText(final String s, final Object... expectedResults) {
        assertNumberMixedWithText(s, true, F, F, expectedResults);
    }

    protected void assertDurationFirst(final String s, final java.time.Duration expectedResult) {
        assertDurationFirst(s, true, expectedResult);
    }

    private static String longNumberMixedWithText;
    private static int partsOfLongNumberMixedWithText;

    @BeforeClass
    public static void setupLongNumberMixedWithText() {
        final ParserFormatter npf = new ParserFormatter(new FrenchFormatter(), null);
        final List<String> strings = new ArrayList<>();
        for (int i = 0; i < 1100000000;) {
            if (i < 2200) {
                ++i;
            } else if (i < 1000000) {
                i += 1207;
            } else {
                i += 299527;
            }

            final double num = (i % 4 == 0) ? (1.0 / i) : i;
            strings.add(npf.pronounceNumber(num).places(0).get());
            strings.add(npf.pronounceNumber(num).places(0).ordinal(T).get());
            strings.add(npf.niceNumber(num).speech(false).get());
            strings.add(npf.niceNumber(num).speech(true).get());
            strings.add(String.valueOf(num));
            strings.add(i % 2 == 0 ? " bonjour " : " de ");
            strings.add(i % 2 == 0 ? "invalide" : "un centième");
            strings.add(i % 2 == 0 ? " et " : " un ");
            strings.add(i % 2 == 0 ? "," : " ; ");
            strings.add("-++-+--+-+-");
            strings.add(i % 2 == 0 ? " plus " : " moins ");
        }
        Collections.shuffle(strings, new Random(42));
        partsOfLongNumberMixedWithText = strings.size();
        longNumberMixedWithText = String.join("", strings);
    }

    @Test
    public void testNumberFirst() {
        assertNumberFirst("un bonjour",       n(1, F));
        assertNumberFirst("dix-neuf euros",   n(19, F));
        assertNumberFirst("premier résultat", n(1, T));
        assertNumberFirst("inconnu",          null);
    }

    @Test
    public void testNumberMixedWithText() {
        assertNumberMixedWithText("vingt et un et cent soixante-quatre",
                n(21, F), " et ", n(164, F));
        assertNumberMixedWithText("bonjour trois monde",  "bonjour ", n(3, F), " monde");
    }

    @Test
    public void testDurationFirst() {
        assertDurationFirst("un minuteur de deux minutes et trente secondes test",
                t(2 * MINUTE + 30));
        assertDurationFirst("il y a deux ans", t(2 * YEAR));
    }

    @Test(timeout = 5000)
    public void testNumberMixedWithTextPerformance() {
        assertEquals(73667, partsOfLongNumberMixedWithText);

        for (int i = 0; i < (1 << 2); ++i) {
            final List<Object> objects = new ExtractNumberParams(numberParser(), longNumberMixedWithText)
                    .integerOnly(i % 2 == 1).preferOrdinal((i / 2) % 2 == 1)
                    .parseMixedWithText();
            assertTrue(objects.size() / ((double) partsOfLongNumberMixedWithText) > 0.8);
        }
    }
}
