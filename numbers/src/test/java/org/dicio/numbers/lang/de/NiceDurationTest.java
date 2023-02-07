package org.dicio.numbers.lang.de;

import static org.dicio.numbers.test.TestUtils.F;
import static org.dicio.numbers.test.TestUtils.T;

import org.dicio.numbers.formatter.NumberFormatter;
import org.dicio.numbers.test.NiceDurationTestBase;
import org.junit.Test;

public class NiceDurationTest extends NiceDurationTestBase {

    @Override
    public NumberFormatter buildNumberFormatter() {
        return new GermanFormatter();
    }

    @Test
    public void zero() {
        assertDuration("null Sekunden", T, 0, 0, 0, 0);
        assertDuration("0:00",          F, 0, 0, 0, 0);
    }

    @Test
    public void speechOne() {
        assertDuration("eine Sekunde", T, 0, 0, 0, 1);
        assertDuration("eine Minute",  T, 0, 0, 1, 0);
        assertDuration("eine Stunde",  T, 0, 1, 0, 0);
        assertDuration("ein Tag",      T, 1, 0, 0, 0);
    }

    @Test
    public void speechMany() {
        assertDuration("fünf Sekunden",       T,  0,  0, 0, 5);
        assertDuration("zwei Minuten",        T,  0,  0, 2, 0);
        assertDuration("siebzehn Stunden",    T,  0, 17, 0, 0);
        assertDuration("vierundachtzig Tage", T, 84,  0, 0, 0);
    }

    @Test
    public void speech() {
        assertDuration("sechs Tage dreiundzwanzig Stunden neunundfünfzig Minuten zweiunddreißig Sekunden", T,  6, 23, 59, 32);
        assertDuration("neunzehn Tage zweiundfünfzig Minuten",                                             T, 19,  0, 52,  0);
        assertDuration("eine Stunde sechs Sekunden",                                                       T,  0,  1,  0,  6);
        assertDuration("dreiundsechzig Tage vierundvierzig Sekunden",                                      T, 63,  0,  0, 44);
        assertDuration("ein Tag eine Stunde eine Minute eine Sekunde",                                     T,  1,  1,  1,  1);
    }

    @Test
    public void noSpeechOne() {
        assertDuration("0:01",       F, 0, 0, 0, 1);
        assertDuration("1:00",       F, 0, 0, 1, 0);
        assertDuration("1:00:00",    F, 0, 1, 0, 0);
        assertDuration("1d 0:00:00", F, 1, 0, 0, 0);
    }

    @Test
    public void noSpeechMany() {
        assertDuration("0:39",        F,  0, 0,  0, 39);
        assertDuration("24:00",       F,  0, 0, 24,  0);
        assertDuration("3:00:00",     F,  0, 3,  0,  0);
        assertDuration("76d 0:00:00", F, 76, 0,  0,  0);
    }

    @Test
    public void noSpeech() {
        assertDuration("6d 23:59:32", F,  6, 23, 59, 32);
        assertDuration("19d 0:52:00", F, 19,  0, 52,  0);
        assertDuration("1:00:06",     F,  0,  1,  0,  6);
        assertDuration("63d 0:00:44", F, 63,  0,  0, 44);
        assertDuration("1d 1:01:01",  F , 1,  1,  1,  1);
    }
}
