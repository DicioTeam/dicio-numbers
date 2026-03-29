package org.dicio.numbers.lang.fr;

import static org.dicio.numbers.test.TestUtils.F;
import static org.dicio.numbers.test.TestUtils.T;

import org.dicio.numbers.formatter.Formatter;
import org.dicio.numbers.test.NiceDurationTestBase;
import org.junit.Test;

public class NiceDurationTest extends NiceDurationTestBase {

    @Override
    public Formatter buildNumberFormatter() {
        return new FrenchFormatter();
    }

    @Test
    public void zero() {
        assertDuration("zéro secondes", T, 0, 0, 0, 0);
        assertDuration("0:00",          F, 0, 0, 0, 0);
    }

    @Test
    public void speechOne() {
        // Note: pronounceNumberDuration(1) returns "un" in French (regardless of gender)
        assertDuration("un seconde", T, 0, 0, 0, 1);
        assertDuration("un minute",  T, 0, 0, 1, 0);
        assertDuration("un heure",   T, 0, 1, 0, 0);
        assertDuration("un jour",    T, 1, 0, 0, 0);
    }

    @Test
    public void speechMultiple() {
        assertDuration("deux secondes",  T, 0, 0, 0, 2);
        assertDuration("cinq minutes",   T, 0, 0, 5, 0);
        assertDuration("trois heures",   T, 0, 3, 0, 0);
        assertDuration("dix jours",      T, 10, 0, 0, 0);
    }

    @Test
    public void noSpeech() {
        assertDuration("0:01",   F, 0, 0, 0, 1);
        assertDuration("1:00",   F, 0, 0, 1, 0);
        assertDuration("1:00:00", F, 0, 1, 0, 0);
    }
}
