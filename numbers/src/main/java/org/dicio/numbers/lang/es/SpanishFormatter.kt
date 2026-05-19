package org.dicio.numbers.lang.es

import org.dicio.numbers.formatter.Formatter
import org.dicio.numbers.unit.MixedFraction
import org.dicio.numbers.util.Utils
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

class SpanishFormatter : Formatter("config/es-es") {

    override fun niceNumber(mixedFraction: MixedFraction, speech: Boolean): String {
        if (speech) {
            val sign = if (mixedFraction.negative) "menos " else ""
            if (mixedFraction.numerator == 0) {
                return sign + pronouncePositive(mixedFraction.whole, false)
            }

            // Spanish context: some fractions are gendered. "un medio" (a half) vs "una media tarta" (a half cake)
            val isFemale = mixedFraction.whole == 0L
            val denominatorString = when (mixedFraction.denominator) {
                2 -> if (isFemale) "media" else "medio"
                else -> {
                    // Ordinals are used for other denominators, e.g., "tercio", "cuarto", "quinto"
                    val ordinal = pronouncePositive(mixedFraction.denominator.toLong(), true)
                    if (mixedFraction.numerator > 1) {
                        // pluralize, e.g. "quinto" -> "quintos"
                        if (ordinal.endsWith("o")) ordinal.dropLast(1) + "os" else ordinal + "s"
                    } else {
                        if (ordinal.endsWith("o")) ordinal.dropLast(1) + "o" else ordinal // e.g. tercio, not tercer
                    }
                }
            }

            val numeratorString = if (mixedFraction.numerator == 1) {
                if (isFemale) "una" else "un"
            } else {
                pronouncePositive(mixedFraction.numerator.toLong(), false)
            }

            return if (mixedFraction.whole == 0L) {
                "$sign$numeratorString $denominatorString"
            } else {
                (sign + pronouncePositive(mixedFraction.whole, false)
                        + " y " + numeratorString + " " + denominatorString)
            }
        } else {
            return niceNumberNotSpeech(mixedFraction)
        }
    }

    override fun pronounceNumber(number: Double, places: Int, shortScale: Boolean, scientific: Boolean, ordinal: Boolean): String {
        if (number == Double.POSITIVE_INFINITY) return "infinito"
        if (number == Double.NEGATIVE_INFINITY) return "menos infinito"
        if (java.lang.Double.isNaN(number)) return "no es un número"

        if (scientific || abs(number) > 999999999999999934463.0) {
            val scientificFormatted = String.format(Locale("es"), "%E", number)
            val parts = scientificFormatted.split("E".toRegex(), limit = 2).toTypedArray()
            val power = parts[1].toInt().toDouble()
            if (power != 0.0) {
                val n = parts[0].toDouble()
                return String.format(
                    "%s por diez a la %s",
                    pronounceNumber(n, places, shortScale, false, false),
                    pronounceNumber(power, places, shortScale, false, true) // exponent as ordinal
                )
            }
        }

        val result = StringBuilder()
        var varNumber = number
        if (varNumber < 0) {
            varNumber = -varNumber
            if (places != 0 || varNumber >= 0.5) result.append("menos ")
        }

        val realPlaces = Utils.decimalPlacesNoFinalZeros(varNumber, places)
        val numberIsWhole = realPlaces == 0
        val numberLong = varNumber.toLong() + (if (varNumber % 1 >= 0.5 && numberIsWhole) 1 else 0)

        result.append(pronouncePositive(numberLong, ordinal && numberIsWhole))

        if (realPlaces > 0) {
            if (result.toString() == "menos " || result.isEmpty()) result.append("cero")
            // Spanish context: "coma" is the standard decimal separator
            result.append(" coma")
            val fractionalPart = String.format("%." + realPlaces + "f", varNumber % 1)
            for (i in 2 until fractionalPart.length) {
                result.append(" ")
                result.append(NUMBER_NAMES[(fractionalPart[i].code - '0'.code).toLong()])
            }
        }
        return result.toString()
    }

    private fun pronouncePositive(n: Long, ordinal: Boolean): String {
        if (ordinal) {
            ORDINAL_NAMES[n]?.let { return it }
            // Logic to build compound ordinals can be added here if needed
        } else {
            NUMBER_NAMES[n]?.let { return it }
        }
        
        return when {
            n >= 1_000_000_000_000 -> buildString { val base = if (n / 1_000_000_000_000 == 1L) "" else pronouncePositive(n / 1_000_000_000_000, false) + " "; append(base); append("billones"); val rem = n % 1_000_000_000_000; if (rem > 0) append(" ").append(pronouncePositive(rem, false)) }
            n >= 1_000_000 -> buildString { val base = if (n / 1_000_000 == 1L) "un" else pronouncePositive(n / 1_000_000, false); append(base); append(" millones"); val rem = n % 1_000_000; if (rem > 0) append(" ").append(pronouncePositive(rem, false)) }
            n >= 1000 -> buildString { if (n / 1000 > 1) append(pronouncePositive(n / 1000, false)); append(" mil"); val rem = n % 1000; if (rem > 0) append(" ").append(pronouncePositive(rem, false)) }
            n >= 100 -> buildString { append(HUNDRED_NAMES[n / 100 * 100]); val rem = n % 100; if (rem > 0) append(" ").append(pronouncePositive(rem, false)) }
            n >= 30 -> buildString { append(NUMBER_NAMES[n / 10 * 10]); val rem = n % 10; if (rem > 0) append(" y ").append(pronouncePositive(rem, false)) }
            else -> "" // Should be unreachable given the initial checks
        }
    }

    override fun niceTime(time: LocalTime, speech: Boolean, use24Hour: Boolean, showAmPm: Boolean): String {
        if (speech) {
            if (time.hour == 0 && time.minute == 0) return "medianoche"
            if (time.hour == 12 && time.minute == 0) return "mediodía"
            
            val result = StringBuilder()
            // Spanish context: hours use 1-12 cycle for speech, not 0-23.
            val hourForSpeech = if (use24Hour) time.hour else (if (time.hour % 12 == 0) 12 else time.hour % 12)

            if (time.minute == 45 && !use24Hour) {
                // Spanish context: "menos cuarto" refers to the next hour. "Son las dos menos cuarto" is 1:45.
                val nextHour = (hourForSpeech % 12) + 1
                result.append(getHourName(nextHour, true)).append(" menos cuarto")
            } else {
                result.append(getHourName(hourForSpeech, false))
                when (time.minute) {
                    0 -> result.append(" en punto")
                    15 -> result.append(" y cuarto")
                    30 -> result.append(" y media")
                    else -> result.append(" y ").append(pronouncePositive(time.minute.toLong(), false))
                }
            }

            if (showAmPm && !use24Hour) {
                when {
                    time.hour < 6 -> result.append(" de la madrugada")
                    time.hour < 12 -> result.append(" de la mañana")
                    time.hour < 20 -> result.append(" de la tarde")
                    else -> result.append(" de la noche")
                }
            }
            return result.toString()
        } else {
            val pattern = if (use24Hour) "HH:mm" else if (showAmPm) "h:mm a" else "h:mm"
            return time.format(DateTimeFormatter.ofPattern(pattern, Locale("es", "ES")))
        }
    }

    override fun niceDateTime(dateTime: LocalDateTime, speech: Boolean, use24Hour: Boolean, showAmPm: Boolean): String {
        return super.niceDateTime(dateTime, speech, use24Hour, showAmPm)
            .replace("a las mediodía", "al mediodía")
            .replace("a las medianoche", "a la medianoche")
    }
    
    private fun getHourName(hour: Int, isForNextHour: Boolean): String {
        // Spanish context: "la una" (one o'clock) is feminine singular.
        // All other hours are feminine plural: "las dos", "las tres", etc.
        val normalizedHour = if (hour == 0) 12 else hour
        return if (normalizedHour == 1) {
             "la una"
        } else {
            "las " + pronouncePositive(normalizedHour.toLong(), false)
        }
    }
    
    // "pronounceNumberDuration" is a simplification for contexts where gender doesn't matter,
    // like "un minuto", but "una hora". The base "pronouncePositive" is more versatile.
    override fun pronounceNumberDuration(number: Long): String {
        if (number == 1L) return "un"
        return pronouncePositive(number, false)
    }

    companion object {
        private val NUMBER_NAMES = mapOf(
            0L to "cero", 1L to "uno", 2L to "dos", 3L to "tres", 4L to "cuatro", 5L to "cinco",
            6L to "seis", 7L to "siete", 8L to "ocho", 9L to "nueve", 10L to "diez",
            11L to "once", 12L to "doce", 13L to "trece", 14L to "catorce", 15L to "quince",
            16L to "dieciséis", 17L to "diecisiete", 18L to "dieciocho", 19L to "diecinueve",
            20L to "veinte", 21L to "veintiuno", 22L to "veintidós", 23L to "veintitrés", 24L to "veinticuatro",
            25L to "veinticinco", 26L to "veintiséis", 27L to "veintisiete", 28L to "veintiocho", 29L to "veintinueve",
            30L to "treinta", 40L to "cuarenta", 50L to "cincuenta", 60L to "sesenta", 70L to "setenta",
            80L to "ochenta", 90L to "noventa", 100L to "cien"
        )
        // Spanish context: Hundreds have special names, e.g., 500 is "quinientos", not "cinco cientos".
        private val HUNDRED_NAMES = mapOf(
            100L to "ciento", 200L to "doscientos", 300L to "trescientos", 400L to "cuatrocientos", 500L to "quinientos",
            600L to "seiscientos", 700L to "setecientos", 800L to "ochocientos", 900L to "novecientos"
        )
        // Includes common ordinals.
        private val ORDINAL_NAMES = mapOf(
            1L to "primero", 2L to "segundo", 3L to "tercero", 4L to "cuarto", 5L to "quinto",
            6L to "sexto", 7L to "séptimo", 8L to "octavo", 9L to "noveno", 10L to "décimo",
            11L to "undécimo", 12L to "duodécimo"
        )
    }
}