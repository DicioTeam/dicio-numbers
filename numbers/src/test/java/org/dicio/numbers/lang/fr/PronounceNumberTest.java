package org.dicio.numbers.lang.fr;

import org.dicio.numbers.ParserFormatter;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.dicio.numbers.test.TestUtils.T;
import static org.junit.Assert.assertEquals;

public class PronounceNumberTest {

    private static ParserFormatter pf;

    @BeforeClass
    public static void setup() {
        pf = new ParserFormatter(new FrenchFormatter(), null);
    }

    @Test
    public void smallIntegers() {
        assertEquals("zéro",   pf.pronounceNumber(0).get());
        assertEquals("un",     pf.pronounceNumber(1).get());
        assertEquals("dix",    pf.pronounceNumber(10).get());
        assertEquals("quinze", pf.pronounceNumber(15).get());
        assertEquals("vingt",  pf.pronounceNumber(20).get());
        assertEquals("vingt-sept", pf.pronounceNumber(27).get());
        assertEquals("trente", pf.pronounceNumber(30).get());
        assertEquals("trente-trois", pf.pronounceNumber(33).get());
    }

    @Test
    public void negativeSmallIntegers() {
        assertEquals("moins un",           pf.pronounceNumber(-1).get());
        assertEquals("moins dix",          pf.pronounceNumber(-10).get());
        assertEquals("moins quinze",       pf.pronounceNumber(-15).get());
        assertEquals("moins vingt",        pf.pronounceNumber(-20).get());
        assertEquals("moins vingt-sept",   pf.pronounceNumber(-27).get());
        assertEquals("moins trente",       pf.pronounceNumber(-30).get());
        assertEquals("moins trente-trois", pf.pronounceNumber(-33).get());
    }

    @Test
    public void decimals() {
        assertEquals("zéro virgule zéro cinq", pf.pronounceNumber(0.05).get());
        assertEquals("moins zéro virgule zéro cinq", pf.pronounceNumber(-0.05).get());
        assertEquals("un virgule deux trois", pf.pronounceNumber(1.234).get());
    }

    @Test
    public void specialFrenchNumbers() {
        assertEquals("soixante-dix",       pf.pronounceNumber(70).get());
        assertEquals("soixante et onze",   pf.pronounceNumber(71).get());
        assertEquals("soixante-douze",     pf.pronounceNumber(72).get());
        assertEquals("soixante-dix-neuf",  pf.pronounceNumber(79).get());
        assertEquals("quatre-vingts",      pf.pronounceNumber(80).get());
        assertEquals("quatre-vingt-un",    pf.pronounceNumber(81).get());
        assertEquals("quatre-vingt-dix",   pf.pronounceNumber(90).get());
        assertEquals("quatre-vingt-onze",  pf.pronounceNumber(91).get());
        assertEquals("quatre-vingt-dix-neuf", pf.pronounceNumber(99).get());
    }

    @Test
    public void hundreds() {
        assertEquals("cent",         pf.pronounceNumber(100).get());
        assertEquals("deux cents",   pf.pronounceNumber(200).get());
        assertEquals("deux cent un", pf.pronounceNumber(201).get());
        assertEquals("cinq cent soixante-dix-huit", pf.pronounceNumber(578).get());
    }

    @Test
    public void thousands() {
        assertEquals("mille",           pf.pronounceNumber(1000).get());
        assertEquals("deux mille",      pf.pronounceNumber(2000).get());
        assertEquals("mille un",        pf.pronounceNumber(1001).get());
        assertEquals("deux mille vingt et un", pf.pronounceNumber(2021).get());
    }

    @Test
    public void ordinals() {
        assertEquals("premier",      pf.pronounceNumber(1).ordinal(T).get());
        assertEquals("deuxième",     pf.pronounceNumber(2).ordinal(T).get());
        assertEquals("troisième",    pf.pronounceNumber(3).ordinal(T).get());
        assertEquals("cinquième",    pf.pronounceNumber(5).ordinal(T).get());
        assertEquals("neuvième",     pf.pronounceNumber(9).ordinal(T).get());
        assertEquals("dixième",      pf.pronounceNumber(10).ordinal(T).get());
    }

    @Test
    public void specialValues() {
        assertEquals("infini",         pf.pronounceNumber(Double.POSITIVE_INFINITY).get());
        assertEquals("moins infini",   pf.pronounceNumber(Double.NEGATIVE_INFINITY).get());
        assertEquals("pas un nombre",  pf.pronounceNumber(Double.NaN).get());
    }

    @Test
    public void millions() {
        assertEquals("un million",        pf.pronounceNumber(1000000).get());
        assertEquals("deux millions",     pf.pronounceNumber(2000000).get());
        assertEquals("un milliard",       pf.pronounceNumber(1000000000).get());
        assertEquals("deux milliards",    pf.pronounceNumber(2000000000).get());
    }
}
