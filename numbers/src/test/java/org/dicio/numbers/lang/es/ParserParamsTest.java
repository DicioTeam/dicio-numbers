package org.dicio.numbers.lang.es;

import static org.dicio.numbers.test.TestUtils.DAY;
import static org.dicio.numbers.test.TestUtils.F;
import static org.dicio.numbers.test.TestUtils.HOUR;
import static org.dicio.numbers.test.TestUtils.MILLIS;
import static org.dicio.numbers.test.TestUtils.MINUTE;
import static org.dicio.numbers.test.TestUtils.T;
import static org.dicio.numbers.test.TestUtils.YEAR;
import static org.dicio.numbers.test.TestUtils.n;
import static org.dicio.numbers.test.TestUtils.t;

import org.dicio.numbers.parser.Parser;
import org.dicio.numbers.parser.param.NumberParserParamsTestBase;
import org.junit.Test;

public class ParserParamsTest extends NumberParserParamsTestBase {

    @Override
    protected Parser numberParser() {
        return new SpanishParser();
    }

    @Test
    public void testNumberFirst() {
        // NOTE (ES): Spanish uses long scale, so "trillonésima" is 10^-18.
        assertNumberFirst("es mil novecientos sesenta y cuatro trillonésimas", T, F, n(1964e-18, F));
        assertNumberFirst("treinta y seis doceavos de manzana", F, T, n(3, F));
        assertNumberFirst("soy realmente el ciento ocho", F, F, n(100, F));
        assertNumberFirst("soy realmente el ciento ocho", T, T, n(108, T));
    }

    @Test
    public void testNumberMixedWithText() {
        assertNumberMixedWithText(" hola  qué tal!, 3/5 o cuatro séptimos?", T, F, " hola  qué tal!, ", n(3.0 / 5.0, F), " o ", n(4.0 / 7.0, F), "?");
        assertNumberMixedWithText(" hola  qué tal!, cuatro séptimos o 3/5?", T, T, " hola  qué tal!, ", n(4.0 / 7.0, F), " o ", n(3.0 / 5.0, F), "?");
        // NOTE (ES): "tres milmillonésimo" (three billionth in short scale) is not standard. Using long scale.
        // "tres billonésimo" -> 3 * 10^-12.
        assertNumberMixedWithText("tres billonésimo más dos", T, T, n(3e-12, T), " más ", n(2, F));
        // NOTE (ES): "un billón" is 10^12.
        assertNumberMixedWithText("un billón y mil seiscientos sesenta y cuatro", F, F, n(1e12, F), " y ", n(1664, F));
        assertNumberMixedWithText("dos billonésimas menos cincuenta y ocho", F, T, n(2e-12, T), " menos ", n(-58, F));
        assertNumberMixedWithText("nueve milmillonésimas por once", F, F, n(9e-9, F), " por ", n(11, F));
        assertNumberMixedWithText("tres mitades, no once cuartos", F, T, n(1.5, F), ", no ", n(2.75, F));
        assertNumberMixedWithText("seis pares es igual a una docena ", F, T, n(12, F), " es igual a ", n(12, F), " ");
        assertNumberMixedWithText("una docena de veintenas no es una centena", F, T, n(240, F), " no es ", n(100, F));
        assertNumberMixedWithText("tengo veintitrés años.", T, F, "tengo ", n(23, F), " años.");
        // NOTE (ES): "quintillionth" (short scale) translates to "trillonésimo" (long scale).
        assertNumberMixedWithText("El trillonésimo", F, F, "El ", n(1e18, T));
        assertNumberMixedWithText("Un trillonésimo", T, F, n(1e-18, F));
    }

    @Test
    public void testDurationFirst() {
        // NOTE (ES): "mil millones" is 10^9.
        assertDurationFirst("Pon un temporizador de dos minutos y mil millones de nanosegundos", F, t(2 * MINUTE + 1000L)); // 10^9 ns = 1s
        assertDurationFirst("sabes que hace dos años no son mil millones de días", T, t(2 * YEAR));
    }

    @Test
    public void testDurationMixedWithText() {
        assertDurationMixedWithText("2ns y cuatro horas mientras seis milisegundos.", F, t(4 * HOUR, 2), " mientras ", t(0, 6 * MILLIS), ".");
        assertDurationMixedWithText("sabes que hace dos años no son mil millones de días", T, "sabes que ", t(-2 * YEAR), " no son ", t(1000000000L * DAY));
    }
}