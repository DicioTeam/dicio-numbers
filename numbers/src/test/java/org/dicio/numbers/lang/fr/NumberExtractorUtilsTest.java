package org.dicio.numbers.lang.fr;

import static org.dicio.numbers.test.TestUtils.F;
import static org.dicio.numbers.test.TestUtils.T;

import org.dicio.numbers.test.NumberExtractorUtilsTestBase;
import org.junit.Test;

public class NumberExtractorUtilsTest extends NumberExtractorUtilsTestBase {

    @Override
    public String configFolder() {
        return "config/fr-fr";
    }

    @Test
    public void testNumberLessThan1000() {
        assertNumberLessThan1000("zéro",        T, 0,   F, 1);
        assertNumberLessThan1000("un",           F, 1,   F, 1);
        assertNumberLessThan1000("cinq",         T, 5,   F, 1);
        assertNumberLessThan1000("dix-neuf",     F, 19,  F, 1);
        assertNumberLessThan1000("cent",         T, 100, F, 1);
        assertNumberLessThan1000("trois cent",   T, 300, F, 2);
        assertNumberLessThan1000("vingt-deux",   T, 22,  F, 1);
        assertNumberLessThan1000("soixante-dix", T, 70,  F, 1);
        assertNumberLessThan1000("quatre-vingts", T, 80, F, 1);
        assertNumberLessThan1000("quatre-vingt-dix", T, 90, F, 1);
        assertNumberLessThan1000("quatre-vingt-onze", T, 91, F, 1);
    }

    @Test
    public void testNumberLessThan1000Null() {
        assertNumberLessThan1000Null("bonjour", T);
        assertNumberLessThan1000Null("",        F);
    }
}
