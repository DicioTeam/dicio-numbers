package org.dicio.numbers.unit

import java.util.Objects
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

/**
 * TODO add documentation
 */
class Number private constructor(
    @JvmField val isDecimal: Boolean,
    private val integerValue: Long,
    private val decimalValue: Double,
    val isOrdinal: Boolean
) {
    @JvmOverloads
    constructor(integerValue: Long, isOrdinal: Boolean = false) : this(
        false,
        integerValue,
        Double.NaN,
        isOrdinal
    )

    @JvmOverloads
    constructor(decimalValue: Double, isOrdinal: Boolean = false) : this(
        true,
        0,
        decimalValue,
        isOrdinal
    )


    val isInteger: Boolean
        get() = !isDecimal

    val isZero: Boolean
        get() = (isDecimal && decimalValue == 0.0) || (!isDecimal && integerValue == 0L)

    fun integerValue(): Long {
        return integerValue
    }

    fun decimalValue(): Double {
        return decimalValue
    }

    fun withOrdinal(isOrdinal: Boolean): Number {
        return Number(this.isDecimal, this.integerValue, this.decimalValue, isOrdinal)
    }


    fun multiply(integer: Long): Number {
        return if (isDecimal) {
            Number(decimalValue * integer, isOrdinal)
        } else {
            try {
                Number(Math.multiplyExact(integerValue, integer), isOrdinal)
            } catch (e: ArithmeticException) {
                Number(integerValue.toDouble() * integer, isOrdinal)
            }
        }
    }

    fun multiply(decimal: Double): Number {
        return Number(
            (if (isDecimal) decimalValue else integerValue.toDouble()) * decimal,
            isOrdinal
        )
    }

    fun multiply(number: Number?): Number {
        return (if (number!!.isDecimal) multiply(number.decimalValue) else multiply(
            number.integerValue
        ))
    }

    fun plus(integer: Long): Number {
        return if (isDecimal) {
            Number(decimalValue + integer, isOrdinal)
        } else {
            try {
                Number(Math.addExact(integerValue, integer), isOrdinal)
            } catch (e: ArithmeticException) {
                Number(integerValue.toDouble() + integer, isOrdinal)
            }
        }
    }

    fun plus(decimal: Double): Number {
        return Number(
            (if (isDecimal) decimalValue else integerValue.toDouble()) + decimal,
            isOrdinal
        )
    }

    fun plus(number: Number?): Number {
        return if (number!!.isDecimal) plus(number.decimalValue) else plus(number.integerValue)
    }

    fun divide(integer: Long): Number {
        return if (isDecimal) {
            Number(decimalValue / integer, isOrdinal)
        } else if (integerValue % integer == 0L) {
            Number(integerValue / integer, isOrdinal)
        } else {
            Number((integerValue.toDouble()) / integer, isOrdinal)
        }
    }

    fun divide(decimal: Double): Number {
        return Number(
            (if (isDecimal) decimalValue else integerValue.toDouble()) / decimal,
            isOrdinal
        )
    }

    fun divide(number: Number): Number {
        return if (number.isDecimal) divide(number.decimalValue) else divide(number.integerValue)
    }

    fun lessThan(integer: Long): Boolean {
        return if (isDecimal) (decimalValue < integer) else (integerValue < integer)
    }

    fun lessThan(decimal: Double): Boolean {
        return if (isDecimal) (decimalValue < decimal) else (integerValue < decimal)
    }

    fun moreThan(integer: Long): Boolean {
        return if (isDecimal) (decimalValue > integer) else (integerValue > integer)
    }


    fun equals(integer: Long): Boolean {
        return !isDecimal && integerValue == integer
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        } else if (other == null || javaClass != other.javaClass) {
            return false
        } else {
            val number = other as Number
            return isDecimal == number.isDecimal && isOrdinal == number.isOrdinal && (if (isDecimal
            ) decimalValue == number.decimalValue else integerValue == number.integerValue)
        }
    }

    override fun hashCode(): Int {
        return if (isDecimal) Objects.hash(true, decimalValue) else Objects.hash(
            false,
            integerValue
        )
    }

    override fun toString(): String {
        return ((if (isDecimal) decimalValue.toString() else integerValue.toString())
                + (if (isOrdinal) "th" else ""))
    }

    companion object {
        fun fromObject(o: Any): Number {
            return when (o) {
                is Short, is Int, is Long -> Number((o as kotlin.Number).toLong())
                is Float, is Double -> Number((o as kotlin.Number).toDouble())
                else -> throw IllegalArgumentException(
                        "object is neither an integer nor a decimal number: $o")
            }
        }
    }
}

@OptIn(ExperimentalContracts::class)
fun Number?.isNullOrZero(): Boolean {
    contract {
        returns(false) implies (this@isNullOrZero != null)
    }
    return this == null || this.isZero
}
