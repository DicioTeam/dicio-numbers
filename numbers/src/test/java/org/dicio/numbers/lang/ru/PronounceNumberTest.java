package org.dicio.numbers.lang.ru;

import static org.dicio.numbers.test.TestUtils.T;
import static org.junit.Assert.assertEquals;

import org.dicio.numbers.ParserFormatter;
import org.junit.BeforeClass;
import org.junit.Test;

public class PronounceNumberTest {

    private static ParserFormatter pf;

    @BeforeClass
    public static void setup() {
        pf = new ParserFormatter(new RussianFormatter(), null);
    }

    @Test
    public void cardinals() {
        assertEquals("ноль", pf.pronounceNumber(0).get());
        assertEquals("один", pf.pronounceNumber(1).get());
        assertEquals("двадцать один", pf.pronounceNumber(21).get());
        assertEquals("сто двенадцать", pf.pronounceNumber(112).get());
        assertEquals("девятьсот девяносто девять", pf.pronounceNumber(999).get());
        assertEquals("тысяча", pf.pronounceNumber(1000).get());
        assertEquals("две тысячи двадцать семь", pf.pronounceNumber(2027).get());
        assertEquals("двадцать одна тысяча", pf.pronounceNumber(21000).get());
        assertEquals("пять тысяч", pf.pronounceNumber(5000).get());
        assertEquals("миллион", pf.pronounceNumber(1000000).get());
        assertEquals("два миллиона триста тысяч", pf.pronounceNumber(2300000).get());
        assertEquals("пять миллионов", pf.pronounceNumber(5000000).get());
        assertEquals("минус пятнадцать", pf.pronounceNumber(-15).get());
    }

    @Test
    public void decimals() {
        assertEquals("три целых четырнадцать сотых", pf.pronounceNumber(3.14).get());
        assertEquals("одна целая пять десятых", pf.pronounceNumber(1.5).get());
        assertEquals("ноль целых одна десятая", pf.pronounceNumber(0.1).get());
        assertEquals("минус две целых двадцать пять сотых", pf.pronounceNumber(-2.25).get());
        assertEquals("три", pf.pronounceNumber(3.14).places(0).get());
    }

    @Test
    public void ordinals() {
        assertEquals("первый", pf.pronounceNumber(1).ordinal(T).get());
        assertEquals("третий", pf.pronounceNumber(3).ordinal(T).get());
        assertEquals("сороковой", pf.pronounceNumber(40).ordinal(T).get());
        assertEquals("двадцать первый", pf.pronounceNumber(21).ordinal(T).get());
        assertEquals("трёхсотый", pf.pronounceNumber(300).ordinal(T).get());
        assertEquals("сто пятый", pf.pronounceNumber(105).ordinal(T).get());
        assertEquals("тысячный", pf.pronounceNumber(1000).ordinal(T).get());
        assertEquals("двухтысячный", pf.pronounceNumber(2000).ordinal(T).get());
        assertEquals("тысяча двадцать первый", pf.pronounceNumber(1021).ordinal(T).get());
        assertEquals("пятимиллионный", pf.pronounceNumber(5000000).ordinal(T).get());
        assertEquals("миллион двухтысячный", pf.pronounceNumber(1002000).ordinal(T).get());
    }
}
