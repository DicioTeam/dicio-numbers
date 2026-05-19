package org.dicio.numbers.lang.es;

import org.dicio.numbers.ParserFormatter;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.dicio.numbers.test.TestUtils.F;
import static org.dicio.numbers.test.TestUtils.T;
import static org.junit.Assert.assertEquals;

public class PronounceNumberTest {

    private static ParserFormatter pf;

    @BeforeClass
    public static void setup() {
        pf = new ParserFormatter(new SpanishFormatter(), null);
    }

    @Test
    public void smallIntegers() {
        assertEquals("cero", pf.pronounceNumber(0).get());
        assertEquals("uno", pf.pronounceNumber(1).get());
        assertEquals("diez", pf.pronounceNumber(10).get());
        assertEquals("quince", pf.pronounceNumber(15).get());
        assertEquals("veinte", pf.pronounceNumber(20).get());
        // NOTE (ES): Numbers from 21-29 are single words in Spanish.
        assertEquals("veintisiete", pf.pronounceNumber(27).get());
        assertEquals("treinta", pf.pronounceNumber(30).get());
        // NOTE (ES): Spanish uses "y" to connect tens and units above 30.
        assertEquals("treinta y tres", pf.pronounceNumber(33).get());
    }

    @Test
    public void negativeSmallIntegers() {
        assertEquals("menos uno", pf.pronounceNumber(-1).get());
        assertEquals("menos diez", pf.pronounceNumber(-10).get());
        assertEquals("menos quince", pf.pronounceNumber(-15).get());
        assertEquals("menos veinte", pf.pronounceNumber(-20).get());
        assertEquals("menos veintisiete", pf.pronounceNumber(-27).get());
        assertEquals("menos treinta", pf.pronounceNumber(-30).get());
        assertEquals("menos treinta y tres", pf.pronounceNumber(-33).get());
    }

    @Test
    public void decimals() {
        // NOTE (ES): Decimal separator is "coma".
        assertEquals("cero coma cero cinco", pf.pronounceNumber(0.05).get());
        assertEquals("menos cero coma cero cinco", pf.pronounceNumber(-0.05).get());
        assertEquals("uno coma dos tres cuatro", pf.pronounceNumber(1.234).get());
        assertEquals("veintiuno coma dos seis cuatro", pf.pronounceNumber(21.264).places(5).get());
        assertEquals("veintiuno coma dos seis cuatro", pf.pronounceNumber(21.264).places(4).get());
        assertEquals("veintiuno coma dos seis cuatro", pf.pronounceNumber(21.264).places(3).get());
        assertEquals("veintiuno coma dos seis", pf.pronounceNumber(21.264).places(2).get());
        assertEquals("veintiuno coma tres", pf.pronounceNumber(21.264).places(1).get());
        assertEquals("veintiuno", pf.pronounceNumber(21.264).places(0).get());
        assertEquals("menos veintiuno coma dos seis cuatro", pf.pronounceNumber(-21.264).places(3).get());
        assertEquals("menos veintiuno coma tres", pf.pronounceNumber(-21.264).places(1).get());
    }

    @Test
    public void roundingDecimals() {
        assertEquals("cero", pf.pronounceNumber(0.05).places(0).get());
        assertEquals("cero", pf.pronounceNumber(-0.4).places(0).get());
        assertEquals("menos veintidós", pf.pronounceNumber(-21.7).places(0).get());
        assertEquals("ochenta y nueve", pf.pronounceNumber(89.2).places(0).get());
        assertEquals("noventa", pf.pronounceNumber(89.9).places(0).get());
        assertEquals("menos uno", pf.pronounceNumber(-0.5).places(0).get());
        assertEquals("seis coma tres", pf.pronounceNumber(6.28).places(1).get());
        assertEquals("tres coma dos", pf.pronounceNumber(3.150001).places(1).get());
        assertEquals("cero coma tres", pf.pronounceNumber(0.25).places(1).get());
        assertEquals("diecinueve", pf.pronounceNumber(19.004).get());
    }

    @Test
    public void hundred() {
        // NOTE (ES): "cien" is used for exactly 100, "ciento" for compounds (e.g., 101 -> "ciento uno").
        assertEquals("cien", pf.pronounceNumber(100).get());
        assertEquals("seiscientos setenta y ocho", pf.pronounceNumber(678).get());
        assertEquals("ciento tres millones doscientos cincuenta y cuatro mil seiscientos cincuenta y cuatro",
                pf.pronounceNumber(103254654).get());
        assertEquals("un millón quinientos doce mil cuatrocientos cincuenta y siete",
                pf.pronounceNumber(1512457).get());
        assertEquals("doscientos nueve mil novecientos noventa y seis",
                pf.pronounceNumber(209996).get());
    }

    @Test
    public void year() {
        // NOTE (ES): Years are typically pronounced fully in Spanish. "nineteen eighty four" is not used.
        assertEquals("mil cuatrocientos cincuenta y seis", pf.pronounceNumber(1456).get());
        assertEquals("mil novecientos ochenta y cuatro", pf.pronounceNumber(1984).get());
        assertEquals("mil ochocientos uno", pf.pronounceNumber(1801).get());
        assertEquals("mil cien", pf.pronounceNumber(1100).get());
        assertEquals("mil doscientos uno", pf.pronounceNumber(1201).get());
        assertEquals("mil quinientos diez", pf.pronounceNumber(1510).get());
        assertEquals("mil seis", pf.pronounceNumber(1006).get());
        assertEquals("mil", pf.pronounceNumber(1000).get());
        assertEquals("dos mil", pf.pronounceNumber(2000).get());
        assertEquals("dos mil quince", pf.pronounceNumber(2015).get());
    }

    @Test
    public void scientificNotation() {
        assertEquals("cero", pf.pronounceNumber(0.0).scientific(T).get());
        assertEquals("tres coma tres por diez a la uno",
                pf.pronounceNumber(33).scientific(T).get());
        assertEquals("dos coma nueve nueve por diez a la ocho",
                pf.pronounceNumber(299492458).scientific(T).get());
        assertEquals("uno coma seis siete dos por diez a la menos veintisiete",
                pf.pronounceNumber(1.672e-27).scientific(T).places(3).get());
    }

    @Test
    public void largeNumbers() {
        // NOTE (ES): Spanish uses the long scale exclusively. Short scale tests are not applicable.
        // millardo = 10^9, billón = 10^12, trillón = 10^18.
        assertEquals("un millón mil ochocientos noventa y dos", pf.pronounceNumber(1001892).get());
        assertEquals("doscientos noventa y nueve millones setecientos noventa y dos mil cuatrocientos cincuenta y ocho", pf.pronounceNumber(299792458).get());
        assertEquals("menos cien mil doscientos dos millones ciento treinta y tres mil cuatrocientos cuarenta", pf.pronounceNumber(-100202133440.0).get());
        assertEquals("veinte billones ciento dos mil millones novecientos ochenta y siete mil", pf.pronounceNumber(20102000987000.0).get());
        assertEquals("siete trillones", pf.pronounceNumber(7000000000000000000.0).get());
        assertEquals("un millón uno", pf.pronounceNumber(1000001).get());
    }

    @Test
    public void ordinal() {
        assertEquals("primero", pf.pronounceNumber(1).ordinal(T).get());
        assertEquals("décimo", pf.pronounceNumber(10).ordinal(T).get());
        assertEquals("decimoquinto", pf.pronounceNumber(15).ordinal(T).get());
        assertEquals("vigésimo", pf.pronounceNumber(20).ordinal(T).get());
        assertEquals("vigesimoséptimo", pf.pronounceNumber(27).ordinal(T).get());
        assertEquals("trigésimo", pf.pronounceNumber(30).ordinal(T).get());
        assertEquals("trigésimo tercero", pf.pronounceNumber(33).ordinal(T).get());
        assertEquals("centésimo", pf.pronounceNumber(100).ordinal(T).get());
        assertEquals("centésimo décimo", pf.pronounceNumber(110).ordinal(T).get());
        assertEquals("milésimo", pf.pronounceNumber(1000).ordinal(T).get());
        assertEquals("diezmilésimo", pf.pronounceNumber(10000).ordinal(T).get());
        assertEquals("millonésimo", pf.pronounceNumber(1000000).ordinal(T).get());
        // NOTE (ES): Decimal numbers are not pronounced as ordinals. The base number is made ordinal.
        assertEquals("tercero", pf.pronounceNumber(2.78).places(0).ordinal(T).get());
        assertEquals("decimonoveno", pf.pronounceNumber(19.004).ordinal(T).get());
    }

    @Test
    public void edgeCases() {
        assertEquals("cero", pf.pronounceNumber(0.0).get());
        assertEquals("cero", pf.pronounceNumber(-0.0).get());
        assertEquals("infinito", pf.pronounceNumber(Double.POSITIVE_INFINITY).get());
        assertEquals("menos infinito", pf.pronounceNumber(Double.NEGATIVE_INFINITY).get());
        assertEquals("no es un número", pf.pronounceNumber(Double.NaN).get());
    }
}