package org.dicio.numbers.lang.sv;

import static org.dicio.numbers.test.TestUtils.F;
import static org.dicio.numbers.test.TestUtils.T;

import org.dicio.numbers.formatter.Formatter;
import org.dicio.numbers.test.NiceDurationTestBase;
import org.junit.Test;

public class NiceDurationTest extends NiceDurationTestBase {

    @Override
    public Formatter buildNumberFormatter() {
        return new SwedishFormatter();
    }

    @Test
    public void zero() {
        assertDuration("noll sekunder", T, 0, 0, 0, 0);
        assertDuration("0:00",          F, 0, 0, 0, 0);
    }

    @Test
    public void speechOne() {
        assertDuration("en sekund", T, 0, 0, 0, 1);
        assertDuration("en minut",  T, 0, 0, 1, 0);
        assertDuration("en timme",  T, 0, 1, 0, 0);
        assertDuration("en dag",    T, 1, 0, 0, 0);
    }

    @Test
    public void speechMany() {
        assertDuration("fem sekunder",      T,  0,  0, 0, 5);
        assertDuration("två minuter",       T,  0,  0, 2, 0);
        assertDuration("sjutton timmar",    T,  0, 17, 0, 0);
        assertDuration("åttiofyra dagar",   T, 84,  0, 0, 0);
    }

    @Test
    public void speech() {
        assertDuration("sex dagar tjugotre timmar femtionio minuter trettiotvå sekunder", T, 6, 23, 59, 32);
        assertDuration("en timme sex sekunder",                                           T, 0,  1,  0,  6);
        assertDuration("en dag en timme en minut en sekund",                              T, 1,  1,  1,  1);
        assertDuration("tjugoen dagar",                                                   T, 21, 0,  0,  0);
    }

    @Test
    public void noSpeech() {
        assertDuration("0:01",       F, 0, 0, 0, 1);
        assertDuration("1:00",       F, 0, 0, 1, 0);
        assertDuration("1:00:00",    F, 0, 1, 0, 0);
        assertDuration("1d 0:00:00", F, 1, 0, 0, 0);
        assertDuration("3d 4:05:06", F, 3, 4, 5, 6);
    }
}
