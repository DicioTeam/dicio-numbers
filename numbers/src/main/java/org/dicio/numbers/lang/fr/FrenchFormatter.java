package org.dicio.numbers.lang.fr;

import java.time.LocalTime;
import java.util.*;

import org.dicio.numbers.formatter.NumberFormatter;
import org.dicio.numbers.util.MixedFraction;
import org.dicio.numbers.util.Utils;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class FrenchFormatter extends NumberFormatter {
    //FIXME:
    final Map<Long, String> NUMBER_NAMES = new HashMap<Long, String>() {{}};
    final Map<Long, String> NUMBER_NAMES_SHORT_SCALE = new HashMap<Long, String>(NUMBER_NAMES) {{}};
    final Map<Long, String> NUMBER_NAMES_LONG_SCALE = new HashMap<Long, String>(NUMBER_NAMES) {{}};
    final Map<Long, String> ORDINAL_NAMES = new HashMap<Long, String>() {{}};
    final Map<Long, String> ORDINAL_NAMES_SHORT_SCALE = new HashMap<Long, String>(ORDINAL_NAMES) {{}};
    final Map<Long, String> ORDINAL_NAMES_LONG_SCALE = new HashMap<Long, String>(ORDINAL_NAMES) {{}};


    protected FrenchFormatter() {
        super("config/fr-fr");
    }


    // Copied from EnglishFormatter with no changes except for strings.
    @Override
    public String niceNumber(MixedFraction mixedFraction, boolean speech) {
        if (speech) {
            final String sign = mixedFraction.negative ? "moins " : "";
            if (mixedFraction.numerator == 0) {
                return sign + pronounceNumber(mixedFraction.whole, 0, true, false, false);
            }

            String denominatorString;
            if (mixedFraction.denominator == 2) {
                denominatorString = "moitié"; //FIXME: Should it be "demi" instead?
            } else if (mixedFraction.denominator == 4) {
                denominatorString = "quart";
            } else {
                // use ordinal: only half and quarter are exceptions
                denominatorString
                        = pronounceNumber(mixedFraction.denominator, 0, true, false, true);
            }

            final String numeratorString;
            if (mixedFraction.numerator == 1) {
                numeratorString = "un"; //FIXME: Should it be empty? 2/1 = 2, not 2 over one
            } else {
                numeratorString = pronounceNumber(mixedFraction.numerator, 0, true, false, false);
                denominatorString += "s";
            }

            if (mixedFraction.whole == 0) {
                return sign + numeratorString + " " + denominatorString;
            } else {
                return sign + pronounceNumber(mixedFraction.whole, 0, true, false, false)
                        + " et " + numeratorString + " " + denominatorString;
            }

        } else {
            return niceNumberNotSpeech(mixedFraction);
        }
    }

    // Copied from EnglishFormatter with no changes except for strings.
    @Override
    public String pronounceNumber(double number, int places, boolean shortScale, boolean scientific, boolean ordinal) {
        if (number == Double.POSITIVE_INFINITY) {
            return "infini";
        } else if (number == Double.NEGATIVE_INFINITY) {
            return "moins l'infini";
        } else if (Double.isNaN(number)) {
            return "non défini";
        }

        // also using scientific mode if the number is too big to be spoken fully. Checking against
        // the biggest double smaller than 10^21 = 1000 * 10^18, which is the biggest pronounceable
        // number, since e.g. 999.99 * 10^18 can be pronounced correctly.
        if (scientific || Math.abs(number) > 999999999999999934463d) {
            final String scientificFormatted = String.format(Locale.FRANCE, "%E", number);
            final String[] parts = scientificFormatted.split("E", 2);
            final double power = Integer.parseInt(parts[1]);

            if (power != 0) {
                // This handles negatives of powers separately from the normal
                // handling since each call disables the scientific flag
                final double n = Double.parseDouble(parts[0]);
                return String.format("%s%s fois dix puissance %s%s",
                        n < 0 ? "moins " : "",
                        pronounceNumber(Math.abs(n), places, shortScale, false, false),
                        power < 0 ? "moins " : "",
                        pronounceNumber(Math.abs(power), places, shortScale, false, false));
            }
        }

        final StringBuilder result = new StringBuilder();
        if (number < 0) {
            number = -number;
            // from here on number is always positive
            if (places != 0 || number >= 0.5) {
                // do not add minus if number will be rounded to 0
                //result.append(scientific ? "negative " : "minus ");

                // The negative/minus distinction doesn't exist in french in this context.
                result.append("moins");
            }
        }

        final int realPlaces = Utils.decimalPlacesNoFinalZeros(number, places);
        final boolean numberIsWhole = realPlaces == 0;
        // if no decimal places to be printed, numberLong should be the rounded number
        final long numberLong = (long) number + (number % 1 >= 0.5 && numberIsWhole ? 1 : 0);

        if (!ordinal && numberIsWhole && numberLong > 1000 && numberLong < 2000) {
            // deal with 4 digits that can be said like a date, i.e. 1972 => nineteen seventy two

            result.append(NUMBER_NAMES.get(numberLong / 100));
            result.append(" ");
            if (numberLong % 100 == 0) {
                // 1900 => mille neuf cent (thousand nine hundred)
                result.append(NUMBER_NAMES.get(100L));
            } else if (numberLong % 100 < 10 && numberLong % 100 != 0) {
                // 1906 => mille neuf cent six (thousand nine hundred six)
                // We don't have an "oh" separator
                result.append(NUMBER_NAMES.get(numberLong % 10));
            } else if (numberLong % 10 == 0 || numberLong % 100 < 20) {
                // 1960 => nineteen sixty; 1911 => nineteen eleven
                result.append(NUMBER_NAMES.get(numberLong % 100));
            } else {
                // 1961 => nineteen sixty one
                result.append(NUMBER_NAMES.get(numberLong % 100 - numberLong % 10));
                result.append(" ");
                result.append(NUMBER_NAMES.get(numberLong % 10));
            }

            return result.toString();
        }

        if (!ordinal && NUMBER_NAMES.containsKey(numberLong)) {
            if (number > 90) {
                result.append("one ");
            }
            result.append(NUMBER_NAMES.get(numberLong));

        }
        // No short scale system in french.
        /*else if (shortScale) {
            boolean ordi = ordinal && numberIsWhole; // not ordinal if not whole
            final List<Long> groups = Utils.splitByModulus(numberLong, 1000);
            final List<String> groupNames = new ArrayList<>();
            for (int i = 0; i < groups.size(); ++i) {
                final long z = groups.get(i);
                if (z == 0) {
                    continue; // skip 000 groups
                }
                String groupName = subThousand(z, i == 0 && ordi);

                if (i != 0) {
                    final long magnitude = Utils.longPow(1000, i);
                    if (ordi) {
                        // ordi can be true only for the first group (i.e. at the end of the number)
                        if (z == 1) {
                            // remove "one" from first group (e.g. "one billion, millionth")
                            groupName = ORDINAL_NAMES_SHORT_SCALE.get(magnitude);
                        } else {
                            groupName += " " + ORDINAL_NAMES_SHORT_SCALE.get(magnitude);
                        }
                    } else {
                        groupName += " " + NUMBER_NAMES_SHORT_SCALE.get(magnitude);
                    }
                }

                groupNames.add(groupName);
                ordi = false;
            }

            appendSplitGroups(result, groupNames);

        }*/ else {
            boolean ordi = ordinal && numberIsWhole; // not ordinal if not whole in french as well as in english
            final List<Long> groups = Utils.splitByModulus(numberLong, 1000000);
            final List<String> groupNames = new ArrayList<>();
            for (int i = 0; i < groups.size(); ++i) {
                final long z = groups.get(i);
                if (z == 0) {
                    continue; // skip 000000 groups
                }

                String groupName;
                if (z < 1000) {
                    groupName = subThousand(z, i == 0 && ordi);
                } else {
                    groupName = subThousand(z / 1000, false) + " mille";
                    if (z % 1000 != 0) {
                        groupName += (i == 0 ? ", " : " ") + subThousand(z % 1000, i == 0 && ordi);
                    } else if (i == 0 && ordi) {
                        if (z / 1000 == 1) {
                            groupName = "millième"; // remove "one" from "one thousandth"
                        } else {
                            groupName += "ième";
                        }
                    }
                }

                if (i != 0) {
                    final long magnitude = Utils.longPow(1000000, i);
                    if (ordi) {
                        // ordi can be true only for the first group (i.e. at the end of the number)
                        if (z == 1) {
                            // remove "one" from first group (e.g. "one billion, millionth")
                            groupName = ORDINAL_NAMES_LONG_SCALE.get(magnitude);
                        } else {
                            groupName += " " + ORDINAL_NAMES_LONG_SCALE.get(magnitude);
                        }
                    } else {
                        groupName += " " + NUMBER_NAMES_LONG_SCALE.get(magnitude);
                    }
                }

                groupNames.add(groupName);
                ordi = false;
            }

            appendSplitGroups(result, groupNames);
        }

        if (realPlaces > 0) {
            if (number < 1.0 && (result.length() == 0 || "moins ".contentEquals(result))) {
                result.append("zéro"); // nothing was written before
            }
            result.append(" virgule"); // 0,xxx english (comma decimal separator)

            final String fractionalPart = String.format("%." + realPlaces + "f", number % 1);
            for (int i = 2; i < fractionalPart.length(); ++i) {
                result.append(" ");
                result.append(NUMBER_NAMES.get((long) (fractionalPart.charAt(i) - '0')));
            }
        }

        return result.toString();
    }

    /**
     * @param result the string builder to append the space-separated group names to
     * @param groupNames the group names
     */
    private void appendSplitGroups(final StringBuilder result, final List<String> groupNames) {
        if (!groupNames.isEmpty()) {
            result.append(groupNames.get(groupNames.size() - 1));
        }

        for (int i = groupNames.size() - 2; i >= 0; --i) {
            result.append(" "); // Example: 1 000 000
            result.append(groupNames.get(i));
        }
    }

    /**
     * @param n must be 0 <= n <= 999
     * @param ordinal whether to return an ordinal number (usually with -ième if n > 1)
     * @return the string representation of a number smaller than 1000
     */
    private String subThousand(final long n, final boolean ordinal) {
        //FIXME: Implement according to NiceNumberTest.
        throw new NotImplementedException();
    }


        @Override
    public String niceTime(LocalTime time, boolean speech, boolean use24Hour, boolean showAmPm) {
        // TODO Auto-generated method stub
        return null;
    }

}
