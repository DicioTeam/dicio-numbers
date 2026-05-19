package org.dicio.numbers.lang.es;

import static org.dicio.numbers.test.TestUtils.F;
import static org.dicio.numbers.test.TestUtils.T;

import org.dicio.numbers.test.NumberExtractorUtilsTestBase;
import org.junit.Test;

public class NumberExtractorUtilsTest extends NumberExtractorUtilsTestBase {

    @Override
    public String configFolder() {
        return "config/es-es";
    }

    @Test
    public void testNumberLessThan1000() {
        assertNumberLessThan1000("cero", T, 0, F, 1);
        assertNumberLessThan1000("uno", F, 1, F, 1);
        assertNumberLessThan1000("un", F, 1, F, 1);
        assertNumberLessThan1000("cinco", T, 5, F, 1);
        assertNumberLessThan1000("diecinueve", F, 19, F, 1);
        assertNumberLessThan1000("cien", T, 100, F, 1);
        assertNumberLessThan1000("trescientos", T, 300, F, 1);
        assertNumberLessThan1000("veintiséis", F, 26, F, 1);
        // NOTE (ES): Spanish uses "y" to connect tens and units (e.g., treinta y siete).
        assertNumberLessThan1000("treinta y siete", T, 37, F, 3);
        assertNumberLessThan1000("setecientos seis", F, 706, F, 2);
        assertNumberLessThan1000("ochocientos dieciocho", T, 818, F, 2);
    }

    @Test
    public void testNumberLessThan1000Digits() {
        assertNumberLessThan1000("0", F, 0, F, 1);
        assertNumberLessThan1000("1", T, 1, F, 1);
        assertNumberLessThan1000("15", T, 15, F, 1);
        assertNumberLessThan1000("100 diecinueve", F, 100, F, 1);
        assertNumberLessThan1000("3 cientos 8", T, 300, F, 2); // "cientos" is not a number, stops at 3
        assertNumberLessThan1000("72", F, 72, F, 1);
        assertNumberLessThan1000("912", T, 912, F, 1);
        assertNumberLessThan1000("8 ciento 18", F, 818, F, 3);
        assertNumberLessThan1000("ciento 47", F, 147, F, 2);
        assertNumberLessThan1000("sesenta y 7", F, 67, F, 3);
    }

    @Test
    public void testNumberLessThan1000EdgeCases() {
        assertNumberLessThan1000("cuatro cinco", T, 4, F, 1);
        assertNumberLessThan1000("un dos y", F, 1, F, 1);
        assertNumberLessThan1000("uno trece", T, 1, F, 1);
        assertNumberLessThan1000("dieciséis ocho", F, 16, F, 1);
        assertNumberLessThan1000("dieciocho cien", T, 18, F, 1);
        assertNumberLessThan1000("cero cien", F, 0, F, 1);
        assertNumberLessThan1000("sesenta cero", T, 60, F, 1);
        assertNumberLessThan1000("un ciento", F, 100, F, 2);
        assertNumberLessThan1000("uno y ciento", T, 100, F, 3);
        assertNumberLessThan1000("setecientos y seis", F, 706, F, 3);
        assertNumberLessThan1000("ciento noventa y uno", T, 191, F, 4);
    }

    @Test
    public void testNumberLessThan1000Ordinal() {
        assertNumberLessThan1000("quinto", T, 5, T, 1);
        assertNumberLessThan1000("vigésimo sexto", T, 26, T, 2);
        assertNumberLessThan1000("septuagésimo octavo", F, 70, F, 1);
        assertNumberLessThan1000("quincuagésimo noveno", T, 50, T, 1);
        assertNumberLessThan1000("centésimo decimotercero", T, 113, T, 2);
        assertNumberLessThan1000("primer ciento", T, 1, T, 1);
        assertNumberLessThan1000("septingentésimo décimo", T, 700, T, 1);
        assertNumberLessThan1000("987º", T, 987, T, 2);
        assertNumberLessThan1000("23ro", T, 23, T, 2);
        assertNumberLessThan1000("8vo primero", T, 8, F, 1);
        assertNumberLessThan1000("1ro ciento", T, 1, T, 2);
        assertNumberLessThan1000Null("septuagésima", F);
        assertNumberLessThan1000Null("101ro", F);
    }

    @Test
    public void testNumberLessThan1000Null() {
        assertNumberLessThan1000Null("", F);
        assertNumberLessThan1000Null("hola", T);
        assertNumberLessThan1000Null("hola como estas", F);
        assertNumberLessThan1000Null("hola dos y", T);
        assertNumberLessThan1000Null("un millón", T);
        assertNumberLessThan1000Null(" veinte", F);
    }

    @Test
    public void testNumberGroupShortScale() {
        // NOTE (ES): Spanish uses long scale, but this method tests number group composition before multipliers are applied.
        // It tests if "ciento veinte" is parsed as 120 before it's multiplied by "millones".
        assertNumberGroupShortScale("ciento veinte millones", F, 1000000000, 120, F, 2);
        assertNumberGroupShortScale("mil seis", T, 1000000000, 1006, F, 2);
        assertNumberGroupShortScale("seiscientos mil", F, 1000000, 600, F, 1);
        assertNumberGroupShortScale("ciento setenta mil", T, 1000000, 170, F, 2);
        assertNumberGroupShortScale("572 millones", F, 1000000000, 572, F, 1);
        assertNumberGroupShortScale("un millón", T, 1000000000, 1, F, 1);
        assertNumberGroupShortScale(", ciento noventa y uno", F, 1000, 191, F, 4);
    }

    @Test
    public void testNumberGroupShortScaleOrdinal() {
        assertNumberGroupShortScale("setecientos sesenta y cuatro millonésimo", T, 1000000000, 764, T, 4);
        assertNumberGroupShortScale("quinto milmillonésimo", T, 1000000000, 5, T, 1);
        assertNumberGroupShortScale("decimonoveno centésimo", T, 1000000000, 19, F, 1); // "centésimo" is not a multiplier here
        assertNumberGroupShortScaleNull("duodécimo milésimo", F, 1000000000);
    }

    @Test
    public void testNumberGroupShortScaleNull() {
        assertNumberGroupShortScaleNull("", T, 1000000000);
        assertNumberGroupShortScaleNull("hola", F, 1000000);
        assertNumberGroupShortScaleNull("129000", F, 1000000000);
        assertNumberGroupShortScaleNull("ciento seis", F, 999);
        assertNumberGroupShortScaleNull("doce", T, 0);
        assertNumberGroupShortScaleNull("siete mil millones", F, 1000);
        assertNumberGroupShortScaleNull("nueve mil uno", T, 1000); // Should be "nueve mil y uno"
        assertNumberGroupShortScaleNull("ocho millones de personas", F, 1000000);
        assertNumberGroupShortScaleNull(" diez ", T, 1000000);
    }
}