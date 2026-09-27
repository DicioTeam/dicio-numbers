# Number parser and formatter for Dicio assistant
This *multilingual* library implements methods to **extract** numbers, dates or durations from text and **format** numbers, dates or dutations into human-readable words. It is inspired by [Mycroft's lingua-franca](https://github.com/MycroftAI/lingua-franca), from which it borrows some resource files. Currently Italian (it-it), English (en-us) and German (de-de) are supported, and methods to extract dates or durations are still unavailable (though formatting is).

This repository is part of the **Dicio** project. Also check out [`dicio-android`](https://github.com/Stypox/dicio-android), [`dicio-sentences-compiler`](https://github.com/Stypox/dicio-sentences-compiler/) and [`dicio-skill`](https://github.com/Stypox/dicio-skill/). *Open to contributions :-D*

## Adding a language

You will need to translate some resource files, containing words but also regex, and then adapt some Java code, so be prepared for that.

First of all you need to obtain the `language-country` pair for the language you want to add. This is important so that who is using the library can choose the language to use. For example, English is `en-us` and Italian is `it-it`. Let's call this `LANGUAGE_COUNTRY` from now on.

### Resources

Copy the whole folder `numbers/src/main/resources/config/en-us` into a new folder `numbers/src/main/resources/config/LANGUAGE_COUNTRY`. All of the resource files in the new folder should be translated into the new language. **DO NOT** rename any file, just edit their contents.

### `*.word` files

The files named like `ENGLISH_WORD.word` should contain only one line with the lowercase translation of the word `ENGLISH_WORD` in the new language. For example, the English `second.word` contains "second", the Italian one contains "secondo" and the new language one should contain the translation of "second" into that language. These files are **also present in Mycroft's [lingua-franca](https://github.com/MycroftAI/lingua-franca/blob/master/lingua_franca/res/text/), so copy them from there** to save time! Then just check if everything is fine.

### `date_time.json` file

This file contains the data needed to properly *format* dates, and in part also times, though that's also handled elsewhere. This file is **also present in Mycroft's [lingua-franca](https://github.com/MycroftAI/lingua-franca/blob/master/lingua_franca/res/text/), so copy it from there** to save time! Then just check if everything is fine.

Each of the formats provided in this file can (and shall) contain references to other already-formatted strings to be substituted, in the form `{FORMATTED_STRING_NAME}`. For example, English uses `"{formatted_date} at {formatted_time}"` as the way to format together both the date and the time.

#### `"weekday"`, `"date"`, `"month"`, `"number"`

These JSON objects should contain:
- `"weekday"`: a numbered list of how the 7 days in the week are pronounced, where monday is at position **0**
- `"date"`: a numbered list of how the at-most-31 days in the month are pronounced, where the first day is at position **1**
- `"month"`: a numbered list of how the 12 months are pronounced, where january is at position **1**
- `"number"`: a list of `"NUMBER": "PRONOUNCIATION"` pairs. It should contain the pronounciation of all numbers that you want to use in [`"decade_format"`, `"hundreds_format"` and `"thousand_format"`](#decade_format-hundreds_format-thousand_format).

#### `"decade_format"`, `"hundreds_format"`, `"thousand_format"`

These JSON objects should contain a numbered list (starting from 1, but it doesn't matter) of `(regex, format)` pairs, along with a `"default"` format for when none of the regexes match. Each `(regex, format)` pair should be a JSON object:
- `"match"` contains the JSON-escaped pattern to match, starting with `^` and ending with `$`. The pattern will be matched against the part of the year corresponding to `"decade_format"` (only last two digits), `"hundreds_format"` (last three digits) **or** `"thousand_format"` (last four digits).
- `"format"` (and the `"default"` mentioned before) contains the format to apply. Refer to the table below for which substitutions you can do.

In these JSON object you should put partial year formatting results that will be finally used by `"year_format"`. It doesn't matter if for some language, instead of just formatting e.g. the thousands digit in `"thousand_format"`, you also sometimes format the hundreds digit (this is what happens for English). The important thing is that, at the end, `"year_format"` spits out correct results.

This is the table of possible `FORMATTED_STRING_NAME`s you can use when having to do with `"decade_format"`, `"hundreds_format"` or `"thousand_format"`. Check out [NiceYearSubstitutionTableBuilder.java](./numbers/src/main/java/org/dicio/numbers/formatter/datetime/NiceYearSubstitutionTableBuilder.java). The examples are in English and relative to the years "2019" and "3865 b.c.".
<table>
  <tr>
    <th><code>FORMATTED_STRING_NAME</code></th>
    <th>Explanation</th>
    <th>e.g. "2019"</th>
    <th>e.g. "-3865"</th>
  </tr>
  <tr>
    <td><code>x</code></td>
    <td>the last digit of the year</td>
    <td>"nine"</td>
    <td>"five"</td>
  </tr>
  <tr>
    <td><code>xx</code></td>
    <td>the last two digits of the year, if there is a corresponding entry in the <code>"number"</code> list</td>
    <td>"nineteen"</td>
    <td>"" (65 is not special and thus has no entry in the <code>"number"</code> list)</td>
  </tr>
  <tr>
    <td><code>x0</code></td>
    <td>the tens of the year</td>
    <td>"ten"</td>
    <td>"sixty"</td>
  </tr>
  <tr>
    <td><code>x_in_x0</code></td>
    <td>the tens digit of the year</td>
    <td>"one"</td>
    <td>"six"</td>
  </tr>
  <tr>
    <td><code>xxx</code></td>
    <td>the last three digits of the year, if there is a corresponding entry in the <code>"number"</code> list</td>
    <td>"nineteen" (there is no hundreds digit)</td>
    <td>"" (865 is not special and thus has no entry in the <code>"number"</code> list)</td>
  </tr>
  <tr>
    <td><code>x00</code></td>
    <td>the hundreds of the year, if there is a corresponding entry in the <code>"number"</code> list</td>
    <td>"zero" (probably doesn't make much sense)</td>
    <td>"" (English pronounces hundreds by putting "hundred" after the unit, so there is no entry for 800 in the <code>"number"</code> list)</td>
  </tr>
  <tr>
    <td><code>x_in_x00</code> and <code>x_in_0x00</code></td>
    <td>the hundreds digit of the year</td>
    <td>"zero"</td>
    <td>"eight"</td>
  </tr>
  <tr>
    <td><code>xx00</code></td>
    <td>the year with its tens and its units digits set to 0, if there is a corresponding entry in the <code>"number"</code> list</td>
    <td>"" (2000 is not special and thus has no entry in the <code>"number"</code> list)</td>
    <td>"" (3800 is not special and thus has no entry in the <code>"number"</code> list)</td>
  </tr>
  <tr>
    <td><code>xx_in_xx00</code></td>
    <td>the year divided by 100, if there is a corresponding entry in the <code>"number"</code> list</td>
    <td>"twenty"</td>
    <td>"" (38 is not special and thus has no entry in the <code>"number"</code> list)</td>
  </tr>
  <tr>
    <td><code>x000</code></td>
    <td>the thousands of the year, if there is a corresponding entry in the <code>"number"</code> list</td>
    <td>"" (English pronounces thousands by putting "thousand" after the unit, so there is no entry for 2000 in the <code>"number"</code> list)</td>
    <td>"" (same reason, for 3000)</td>
  </tr>
  <tr>
    <td><code>x_in_x000</code></td>
    <td>the thousands digit of the year</td>
    <td>"two"</td>
    <td>"three"</td>
  </tr>
  <tr>
    <td><code>x0_in_x000</code></td>
    <td>the thousands digit of the year, multiplied by 10</td>
    <td>"twenty"</td>
    <td>"thirty"</td>
  </tr>
  <tr>
    <td><code>number</code></td>
    <td>the non-formatted part of the year corresponding to <code>"decade_format"</code> (only last two digits), <code>"hundreds_format"</code> (last three digits) <b>or</b> <code>"thousand_format"</code> (last four digits), to be used for <code>"default"</code> as a fallback</td>
    <td>"19", "19" or "2019"</td>
    <td>"65", "865" or "3865"</td>
  </tr>
</table>

#### `"year_format"`

This JSON object follows the same structure as [`"decade_format"`, `"hundreds_format"`, `"thousand_format"`](#decade_format-hundreds_format-thousand_format), but there is also a `"bc"` field that should contain the translation of the shortened "Before Christ" ("b.c."). In this JSON object you should put how to fully format a number as a year, using the formatted strings already calculated using `"decade_format"`, `"hundreds_format"` and `"thousand_format"`.

The formats have at their disposal the full table from above plus the following items (which are the ones that should actually be used).
<table>
  <tr>
    <th><code>FORMATTED_STRING_NAME</code></th>
    <th>Explanation</th>
    <th>e.g. "2019"</th>
    <th>e.g. "-3865"</th>
  </tr>
  <tr>
    <td><code>formatted_decade</code></td>
    <td>the decade formatted using <code>"decade_format"</code></td>
    <td>"nineteen"</td>
    <td>"sixty five"</td>
  </tr>
  <tr>
    <td><code>formatted_hundreds</code></td>
    <td>the hundreds formatted using <code>"hundreds_format"</code></td>
    <td>"zero hundred" (yeah, it doesn't make sense)</td>
    <td>"eight hundred"</td>
  </tr>
  <tr>
    <td><code>formatted_thousand</code></td>
    <td>the thousands formatted using <code>"thousand_format"</code></td>
    <td>"twenty"</td>
    <td>"thirty eight"</td>
  </tr>
  <tr>
    <td><code>bc</code></td>
    <td>the translation of "b.c." if the year is before Christ, otherwise an empty string</td>
    <td>""</td>
    <td>"b.c."</td>
  </tr>
  <tr>
    <td><code>number</code></td>
    <td>the non-formatted full-year, to be used for <code>"default"</code> as a fallback</td>
    <td>"2019"</td>
    <td>"3865"</td>
  </tr>
</table>

#### `"date_format"`

This JSON object should contain a format in these fields: `"date_full"`, `"date_full_no_year"`, `"date_full_no_year_month"`; and a translation of the field name in these fields: `"today"`, `"tomorrow"`, `"yesterday"`.

The formats have at their disposal this limited table.
<table>
  <tr>
    <th><code>FORMATTED_STRING_NAME</code></th>
    <th>Explanation</th>
    <th>e.g. "Tuesday, 2022/05/03"</th>
  </tr>
  <tr>
    <td><code>day</code></td>
    <td>the name of the day in the month</td>
    <td>"third"</td>
  </tr>
  <tr>
    <td><code>weekday</code></td>
    <td>the name of the day in the week</td>
    <td>"tuesday"</td>
  </tr>
  <tr>
    <td><code>month</code></td>
    <td>the name of the month</td>
    <td>"may"</td>
  </tr>
  <tr>
    <td><code>formatted_year</code></td>
    <td>the year fully formatted using <code>"year_format"</code></td>
    <td>"twenty twenty two"</td>
  </tr>
</table>

#### `"date_time_format"`

This JSON object should contain a format in this only field: `"date_time"`.

The format has at its disposal this limited table.
<table>
  <tr>
    <th><code>FORMATTED_STRING_NAME</code></th>
    <th>Explanation</th>
    <th>e.g. "Tuesday, 2022/05/03 13:22"</th>
  </tr>
  <tr>
    <td><code>formatted_date</code></td>
    <td>the date fully formatted using <code>"date_format"</code></td>
    <td>"tuesday, may second, twenty twenty two"</td>
  </tr>
  <tr>
    <td><code>formatted_time</code></td>
    <td>the time formatted using the java method <code>niceTime</code></td>
    <td>"one twenty two p.m."</td>
  </tr>
</table>

### `tokenizer.json`

This JSON file contains the information the tokenizer uses to generate the token stream corresponding to an input string.

- `"spaces"`: a string containing all characters that should be considered spaces. This usually does not need to be *translated*.
- `"characters_as_word"`: a string containing all characters that should be considered as single-character words, and not as letters possibly part of a bigger word. An example for this is `%`, since the percent sign has a meaning on its own.
- `"compound_word_piece_category"`: the category that you will give in `"number_mappings"` to words that can be part of a compound word. A compound word is a word made of other words all connected together, and the tokenizer will split such big words using only words that can actually be part of compound words. While the value of this could be arbitrary, given that you then use the same value in `"number_mappings"`, there is no reason to set this to something different than "compound_word_piece", which is what is used in Italian. If the language you want to add has no compound words, like English, just don't add this field.
- `"raw_number_categories"`: an array of all the categories that the tokenizer should give to raw numbers when it encounters them. A raw number is e.g. 2384, -392, ...
- `"plural_endings"`: an array of the endings that the tokenizer should try to trim off a word if it can't recognize it otherwise. For example, in English "tens" would match with "ten" because there is "s" in the `"plural_endings"` array.
- `"word_matches"`: an array of JSON objects of this form:
  - `"categories"`: an array of the categories that should be assigned to the words that match any of the values
  - `"values"`: an array of the words that should be assigned all of the categories
- `"number_mappings"`: an array of JSON objects of this form:
  - `"categories"`: an array of the categories that should be assigned to the words that match any of the values
  - `"values"`: an JSON object with the words that should be assigned all of the categories paired with their corresponding (integer or decimal) numerical value
- `"duration_words"`: a JSON object used to pair words with their corresponding duration. The keys in the object should be the durations (formatted as `"number UNIT"`, where `number` is an integer and `UNIT` (uppercase) is one of NANOS, MICROS, MILLIS, SECONDS, MINUTES, HOURS, HALF_DAYS, DAYS, WEEKS, MONTHS, YEARS, DECADES, CENTURIES, MILLENNIA, ERAS), and the values are JSON arrays of words.
- `"duration_restrict_after_number"`: a list of all of the words present in `"duration_words"` that should not be recognized as a duration if they don't come with a number before them in the input. For example, "hello ms" should **not** be interpreted as "hello (1 millisecond)", while "hello millisecond" and "hello 1 ms" should.

### Test resources

Copy the whole folder `numbers/src/test/resources/config/en-us` into a new folder `numbers/src/test/resources/config/LANGUAGE_COUNTRY`. All of the resource files in the new folder are used for testing purposes and should be translated into the new language. **DO NOT** rename any file, just edit their contents.

### `date_time_test.json`

This file contains some tests for the `date_time.json` file. This file is **also present in Mycroft's [lingua-franca](https://github.com/MycroftAI/lingua-franca/blob/master/lingua_franca/res/text/), so copy it from there** to save time! Then just check if everything is fine.

Each of the JSON objects described below contains a numbered list of tests to run.

#### `"test_nice_year"`

These tests are for [`"year_format"`](#year_format). Each test has:
- `"datetime_param"` the datetime in this form: year, month, day, hour, minute, second
- `"bc"` whether the datetime is before Christ (`True`) or not (`False`)
- `"assertEqual"` what the provided datetime should be formatted as

#### `"test_nice_date"`

These tests are for [`"date_format"`](#date_format). Each test has:
- `"datetime_param"` the datetime in this form: year, month, day, hour, minute, second
- `"now"` the datetime corresponding to the "now" time for which to test relative dates (e.g. if `datetime_param=now`, then `today` will be outputted, not the full date), in the same form, or `None` if "now" is unknown or unwanted
- `"assertEqual"` what the provided datetime should be formatted as

#### `"test_nice_date_time"`

These tests are for [`"date_time_format"`](#date_time_format). Each test has:
- `"datetime_param"` the datetime in this form: year, month, day, hour, minute, second
- `"now"` the datetime corresponding to the "now" time for which to test relative dates (e.g. if `datetime_param=now`, then `today` will be outputted, not the full date), in the same form, or `None` if "now" is unknown or unwanted
- `"use_24hour"` whether to use the 24-hour format (`True`) or use the 12-hour one (`False`) (parameter passed to the java method `niceTime`)
- `"use_ampm"` whether to show AM/PM or not (parameter passed to the java method `niceTime`)
- `"assertEqual"` what the provided datetime should be formatted as
