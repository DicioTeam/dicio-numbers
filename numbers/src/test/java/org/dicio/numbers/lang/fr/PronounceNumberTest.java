package org.dicio.numbers.lang.fr;

import org.dicio.numbers.NumberParserFormatter;
import org.dicio.numbers.lang.en.EnglishFormatter;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.dicio.numbers.test.TestUtils.F;
import static org.dicio.numbers.test.TestUtils.T;
import static org.junit.Assert.assertEquals;

public class PronounceNumberTest {

    private static NumberParserFormatter pf;

    @BeforeClass
    public static void setup() {
        pf = new NumberParserFormatter(new EnglishFormatter(), null);
    }

    @Test
    public void smallIntegers() {
        assertEquals("zéro", pf.pronounceNumber(0).get());
        assertEquals("un", pf.pronounceNumber(1).get());
        assertEquals("dix", pf.pronounceNumber(10).get());
        assertEquals("quinze", pf.pronounceNumber(15).get());
        assertEquals("vingt", pf.pronounceNumber(20).get());
        assertEquals("vingt-sept", pf.pronounceNumber(27).get());
        assertEquals("trente", pf.pronounceNumber(30).get());
        assertEquals("trente trois", pf.pronounceNumber(33).get());
    }

    @Test
    public void negativeSmallIntegers() {
        assertEquals("moins un", pf.pronounceNumber(-1).get());
        assertEquals("moins dix", pf.pronounceNumber(-10).get());
        assertEquals("moins quinze", pf.pronounceNumber(-15).get());
        assertEquals("moins vingt", pf.pronounceNumber(-20).get());
        assertEquals("moins vingt-sept", pf.pronounceNumber(-27).get());
        assertEquals("moins trente", pf.pronounceNumber(-30).get());
        assertEquals("moins trente-trois", pf.pronounceNumber(-33).get());
    }

    @Test
    public void decimals() {
        assertEquals("zéro virgule zéro cinq", pf.pronounceNumber(0.05).get());
        assertEquals("moins zéro virgule zéro cinq", pf.pronounceNumber(-0.05).get());
        // In french we mostly say decimals as we would with dozens or with hundreds if there is only three places .
        assertEquals("un virgule vingt-trois", pf.pronounceNumber(1.234).get());
        // vingt-et-un is also acceptable since 1990's orthographical reform.
        assertEquals("vingt et un virgule deux cent soixante-quatre", pf.pronounceNumber(21.264).places(5).get());
        assertEquals("vingt et un virgule deux cent soixante-quatre", pf.pronounceNumber(21.264).places(4).get());
        assertEquals("vingt et un virgule deux cent soixante-quatre", pf.pronounceNumber(21.264).places(3).get());
        assertEquals("vingt et un virgule vingt-six", pf.pronounceNumber(21.264).places(2).get());
        assertEquals("vingt et un virgule trois", pf.pronounceNumber(21.264).places(1).get());
        assertEquals("vingt et un", pf.pronounceNumber(21.264).places(0).get());
        // Same with negative numbers
        assertEquals("moins vingt et un virgule deux cent soixante-quatre", pf.pronounceNumber(-21.264).places(5).get());
        assertEquals("moins vingt et un virgule deux cent soixante-quatre", pf.pronounceNumber(-21.264).places(4).get());
        assertEquals("moins vingt et un virgule deux cent soixante-quatre", pf.pronounceNumber(-21.264).places(3).get());
        assertEquals("moins vingt et un virgule vingt-six", pf.pronounceNumber(-21.264).places(2).get());
        assertEquals("moins vingt et un virgule trois", pf.pronounceNumber(-21.264).places(1).get());
        assertEquals("moins vingt et un", pf.pronounceNumber(-21.264).places(0).get());
    }

    @Test
    public void roundingDecimals() {
        assertEquals("zéro", pf.pronounceNumber(0.05).places(0).get());
        assertEquals("zéro", pf.pronounceNumber(-0.4).places(0).get());
        assertEquals("moins vingt-deux", pf.pronounceNumber(-21.7).places(0).get());
        assertEquals("quatre-vingt neuf", pf.pronounceNumber(89.2).places(0).get());
        assertEquals("quatre-vingt-dix", pf.pronounceNumber(89.9).places(0).get());
        assertEquals("moins un", pf.pronounceNumber(-0.5).places(0).get());
        assertEquals("zéro", pf.pronounceNumber(-0.4).places(0).get());
        assertEquals("six virgue trois", pf.pronounceNumber(6.28).places(1).get());
        assertEquals("moins trois virgule un", pf.pronounceNumber(-3.14).places(1).get());
        // note: 3.15 does not yield "three point two" because of floating point errors
        assertEquals("trois virgule deux", pf.pronounceNumber(3.150001).places(1).get());
        assertEquals("zéro virgule trois", pf.pronounceNumber(0.25).places(1).get());
        assertEquals("moins zéro virgule trois", pf.pronounceNumber(-0.25).places(1).get());
        assertEquals("dix-neuf", pf.pronounceNumber(19.004).get());
    }

    @Test
    public void hundred() {
        assertEquals("cent", pf.pronounceNumber(100).get());
        assertEquals("six cent soixante-dix-huit", pf.pronounceNumber(678).get());

        assertEquals("cent trois millions, deux cent cinquante-quatre mille, six cent cinquante-quatre",
                pf.pronounceNumber(103254654).get());
        assertEquals("un million, cinq cent douze mille, quatre cent cinquante-sept",
                pf.pronounceNumber(1512457).get());
        assertEquals("deux cent neuf mille, neuf cent quatre-vingt-seize",
                pf.pronounceNumber(209996).get());
    }

    @Test
    public void year() {
        assertEquals("mille quatre cent cinquante-six", pf.pronounceNumber(1456).get());
        assertEquals("mille neuf cent quatre-vingt quatre", pf.pronounceNumber(1984).get());
        assertEquals("mille huit-cent un", pf.pronounceNumber(1801).get());
        assertEquals("mille cent", pf.pronounceNumber(1100).get());
        assertEquals("mille deux cent un", pf.pronounceNumber(1201).get());
        assertEquals("mille cinq cent dix", pf.pronounceNumber(1510).get());
        assertEquals("mille six", pf.pronounceNumber(1006).get());
        assertEquals("mille", pf.pronounceNumber(1000).get());
        assertEquals("deux mille", pf.pronounceNumber(2000).get());
        assertEquals("deux mille quinze", pf.pronounceNumber(2015).get());
        assertEquals("quatre mille huit cent vingt-sept", pf.pronounceNumber(4827).get());
    }

    @Test
    public void scientificNotation() {
        assertEquals("zéro", pf.pronounceNumber(0.0).scientific(T).get());
        assertEquals("trois virgule trois fois dix puissance un",
                pf.pronounceNumber(33).scientific(T).get());
        assertEquals("deux virgule neuf fois dix puissance huit",
                pf.pronounceNumber(299492458).scientific(T).get());
        assertEquals("deux virgule quatre-vingt-dix-neuf quatre-vingt-dix-sept vingt-cinq fois dix puissance huit",
                pf.pronounceNumber(299792458).scientific(T).places(6).get());
        assertEquals("un virgule six cent soixante-dix-sept fois dix puissance moins vingt-sept",
                pf.pronounceNumber(1.672e-27).scientific(T).places(3).get());

        // auto scientific notation when number is too big to be pronounced
        assertEquals("deux virgule quatre-vingt-quinze fois dix puissance vingt-quatre",
                pf.pronounceNumber(2.9489e24).get());
    }

    // Inexistant in french
    /*
    private void assertShortLongScale(final double number,
                                      final String shortScale,
                                      final String longScale) {
        assertEquals(shortScale, pf.pronounceNumber(number).shortScale(T).get());
        assertEquals(longScale, pf.pronounceNumber(number).shortScale(F).get());
    }

    @Test
    public void largeNumbers() {
        assertShortLongScale(1001892,
                "one million, one thousand, eight hundred and ninety two",
                "one million, one thousand, eight hundred and ninety two");
        assertShortLongScale(299792458,
                "two hundred and ninety nine million, seven hundred and ninety two thousand, four hundred and fifty eight",
                "two hundred and ninety nine million, seven hundred and ninety two thousand, four hundred and fifty eight");
        assertShortLongScale(-100202133440.0,
                "minus one hundred billion, two hundred and two million, one hundred and thirty three thousand, four hundred and forty",
                "minus one hundred thousand two hundred and two million, one hundred and thirty three thousand, four hundred and forty");
        assertShortLongScale(20102000987000.0,
                "twenty trillion, one hundred and two billion, nine hundred and eighty seven thousand",
                "twenty billion, one hundred and two thousand million, nine hundred and eighty seven thousand");
        assertShortLongScale(-2061000560007060.0,
                "minus two quadrillion, sixty one trillion, five hundred and sixty million, seven thousand, sixty",
                "minus two thousand sixty one billion, five hundred and sixty million, seven thousand, sixty");
        assertShortLongScale(9111202032999999488.0, // floating point errors
                "nine quintillion, one hundred and eleven quadrillion, two hundred and two trillion, thirty two billion, nine hundred and ninety nine million, nine hundred and ninety nine thousand, four hundred and eighty eight",
                "nine trillion, one hundred and eleven thousand two hundred and two billion, thirty two thousand nine hundred and ninety nine million, nine hundred and ninety nine thousand, four hundred and eighty eight");

        assertShortLongScale(29000.0, "twenty nine thousand", "twenty nine thousand");
        assertShortLongScale(301000.0, "three hundred and one thousand", "three hundred and one thousand");
        assertShortLongScale(4000000.0, "four million", "four million");
        assertShortLongScale(50000000.0, "fifty million", "fifty million");
        assertShortLongScale(630000000.0, "six hundred and thirty million", "six hundred and thirty million");
        assertShortLongScale(7000000000.0, "seven billion", "seven thousand million");
        assertShortLongScale(16000000000.0, "sixteen billion", "sixteen thousand million");
        assertShortLongScale(923000000000.0, "nine hundred and twenty three billion", "nine hundred and twenty three thousand million");
        assertShortLongScale(1000000000000.0, "one trillion", "one billion");
        assertShortLongScale(29000000000000.0, "twenty nine trillion", "twenty nine billion");
        assertShortLongScale(308000000000000.0, "three hundred and eight trillion", "three hundred and eight billion");
        assertShortLongScale(4000000000000000.0, "four quadrillion", "four thousand billion");
        assertShortLongScale(52000000000000000.0, "fifty two quadrillion", "fifty two thousand billion");
        assertShortLongScale(640000000000000000.0, "six hundred and forty quadrillion", "six hundred and forty thousand billion");
        assertShortLongScale(7000000000000000000.0, "seven quintillion", "seven trillion");

        // TODO maybe improve this
        assertShortLongScale(1000001, "one million, one", "one million, one");
        assertShortLongScale(-2000000029, "minus two billion, twenty nine", "minus two thousand million, twenty nine");
    }*/

    @Test
    public void ordinal() {
        // small numbers
        assertEquals("premier", pf.pronounceNumber(1).shortScale(T).ordinal(T).get());
        assertEquals("premier", pf.pronounceNumber(1).shortScale(F).ordinal(T).get());
        assertEquals("dixième", pf.pronounceNumber(10).shortScale(T).ordinal(T).get());
        assertEquals("dixième", pf.pronounceNumber(10).shortScale(F).ordinal(T).get());
        assertEquals("quinzième", pf.pronounceNumber(15).shortScale(T).ordinal(T).get());
        assertEquals("quinzième", pf.pronounceNumber(15).shortScale(F).ordinal(T).get());
        assertEquals("vingtième", pf.pronounceNumber(20).shortScale(T).ordinal(T).get());
        assertEquals("vingtième", pf.pronounceNumber(20).shortScale(F).ordinal(T).get());
        assertEquals("vingt-septième", pf.pronounceNumber(27).shortScale(T).ordinal(T).get());
        assertEquals("vingt-septième", pf.pronounceNumber(27).shortScale(F).ordinal(T).get());
        assertEquals("trentième", pf.pronounceNumber(30).shortScale(T).ordinal(T).get());
        assertEquals("trentième", pf.pronounceNumber(30).shortScale(F).ordinal(T).get());
        assertEquals("trente-troisième", pf.pronounceNumber(33).shortScale(T).ordinal(T).get());
        assertEquals("trente-troisième", pf.pronounceNumber(33).shortScale(F).ordinal(T).get());
        assertEquals("centième", pf.pronounceNumber(100).shortScale(T).ordinal(T).get());
        assertEquals("centième", pf.pronounceNumber(100).shortScale(F).ordinal(T).get());
        assertEquals("millième", pf.pronounceNumber(1000).shortScale(T).ordinal(T).get());
        assertEquals("millième", pf.pronounceNumber(1000).shortScale(F).ordinal(T).get());
        assertEquals("dix-millième", pf.pronounceNumber(10000).shortScale(T).ordinal(T).get());
        assertEquals("dix-millième", pf.pronounceNumber(10000).shortScale(F).ordinal(T).get());
        assertEquals("deux-millième", pf.pronounceNumber(200).shortScale(T).ordinal(T).get());
        assertEquals("deux-millième", pf.pronounceNumber(200).shortScale(F).ordinal(T).get());
        assertEquals("huit-mille-quatre-vingtième", pf.pronounceNumber(18691).ordinal(T).shortScale(T).get());
        assertEquals("huit-mille-quatre-vingtième", pf.pronounceNumber(18691).ordinal(T).shortScale(F).get());
        assertEquals("mille-cinq-cent-soixante-septième", pf.pronounceNumber(1567).ordinal(T).shortScale(T).get());
        assertEquals("mille-cinq-cent-soixante-septième", pf.pronounceNumber(1567).ordinal(T).shortScale(F).get());

        // big numbers
        assertEquals("dix-huit-millionième", pf.pronounceNumber(18000000).ordinal(T).get());
        assertEquals("dix-huit-mille-centième", pf.pronounceNumber(18000100).ordinal(T).get());
        assertEquals("cent-vingt-sept-milliardième", pf.pronounceNumber(127000000000.0).ordinal(T).shortScale(T).get());
        assertEquals("deux-cent-un-millionième", pf.pronounceNumber(201000000000.0).ordinal(T).shortScale(F).get());
        //TODO: Check this for french correctness as well as short/long scale issues (billion/billard)
        assertEquals("neuf-cent-treize-milliard-quatre-vingt-million-six-cent-mille-soixante-mille-cent-soixante-quatrième", pf.pronounceNumber(913080600064.0).ordinal(T).shortScale(T).get());
        assertEquals("neuf-cent-treize-mille-quatre-vingt-million-six-cent-mille-soixante-mille-soixante-quatrième", pf.pronounceNumber(913080600064.0).ordinal(T).shortScale(F).get());
        assertEquals("trilliard-deux-millionième", pf.pronounceNumber(1000002000000.0).ordinal(T).shortScale(T).get());
        assertEquals("millard-deux-millionième", pf.pronounceNumber(1000002000000.0).ordinal(T).shortScale(F).get());
        assertEquals("quatre-triliard-un-millionième", pf.pronounceNumber(4000001000000.0).ordinal(T).shortScale(T).get());
        assertEquals("quatre-milliard-un-millionième", pf.pronounceNumber(4000001000000.0).ordinal(T).shortScale(F).get());

        // decimal numbers and scientific notation: the behaviour should be the same as with ordinal=F
        assertEquals("deux virgule soixante-dix-huit", pf.pronounceNumber(2.78).ordinal(T).get());
        assertEquals("tiers", pf.pronounceNumber(2.78).places(0).ordinal(T).get());
        assertEquals("dix-neuf", pf.pronounceNumber(19.004).ordinal(T).get());
        assertEquals("huit cent trente-trois millions, quatre cent vingt-huit mille, quatre-vingt-douze virgule cent quatre-vingt-trois", pf.pronounceNumber(830438092.1829).places(3).ordinal(T).get());
        assertEquals("deux virgule cinquante-quatre fois dix puissance six", pf.pronounceNumber(2.54e6).ordinal(T).scientific(T).get());
    }

    @Test
    public void edgeCases() {
        assertEquals("zéro", pf.pronounceNumber(0.0).get());
        assertEquals("zéro", pf.pronounceNumber(-0.0).get());
        assertEquals("infini", pf.pronounceNumber(Double.POSITIVE_INFINITY).get());
        assertEquals("moins l'infini", pf.pronounceNumber(Double.NEGATIVE_INFINITY).scientific(F).get());
        assertEquals("moins l''infini", pf.pronounceNumber(Double.NEGATIVE_INFINITY).scientific(T).get());
        assertEquals("Non défini", pf.pronounceNumber(Double.NaN).get());
    }
}
