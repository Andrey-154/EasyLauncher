package com.minimo.launcher.utils

import java.math.BigDecimal
import java.math.MathContext
import kotlin.math.pow

/**
 * Tiny calculator for the app drawer search: numbers, + - * / ^, parentheses and
 * percent ("200*15%"). Returns null when the text is not a math expression.
 */
object Calculator {
    private val allowedChars = Regex("""^[\d\s.,+\-*/×÷:^()%]+$""")
    private val hasOperator = Regex("""\d\s*[+\-*/×÷:^%]""")

    fun evaluate(input: String): String? {
        val text = input.trim()
        if (text.isEmpty() || !allowedChars.matches(text) || !hasOperator.containsMatchIn(text)) {
            return null
        }
        return try {
            val parser = Parser(
                text.replace(',', '.')
                    .replace('×', '*')
                    .replace('÷', '/')
                    .replace(':', '/')
                    .replace(" ", "")
            )
            val result = parser.parse()
            if (result.isNaN() || result.isInfinite()) null else format(result)
        } catch (_: Exception) {
            null
        }
    }

    private fun format(value: Double): String {
        val rounded = BigDecimal(value).round(MathContext(12)).stripTrailingZeros()
        return if (rounded.scale() <= 0) rounded.toBigInteger().toString() else rounded.toPlainString()
    }

    /** Recursive descent: expression = term (+|- term)*, term = factor (*|/ factor)*, ... */
    private class Parser(private val s: String) {
        private var pos = 0

        fun parse(): Double {
            val value = expression()
            require(pos == s.length) { "Unexpected character at $pos" }
            return value
        }

        private fun expression(): Double {
            var value = term()
            while (pos < s.length) {
                when (s[pos]) {
                    '+' -> { pos++; value += term() }
                    '-' -> { pos++; value -= term() }
                    else -> return value
                }
            }
            return value
        }

        private fun term(): Double {
            var value = power()
            while (pos < s.length) {
                when (s[pos]) {
                    '*' -> {
                        pos++
                        value *= power()
                    }

                    '/' -> {
                        pos++
                        val divisor = power()
                        require(divisor != 0.0) { "Division by zero" }
                        value /= divisor
                    }

                    else -> return value
                }
            }
            return value
        }

        private fun power(): Double {
            val base = unary()
            if (pos < s.length && s[pos] == '^') {
                pos++
                return base.pow(power())
            }
            return base
        }

        private fun unary(): Double {
            if (pos < s.length && s[pos] == '-') {
                pos++
                return -unary()
            }
            if (pos < s.length && s[pos] == '+') {
                pos++
                return unary()
            }
            return percent()
        }

        private fun percent(): Double {
            var value = primary()
            while (pos < s.length && s[pos] == '%') {
                pos++
                value /= 100.0
            }
            return value
        }

        private fun primary(): Double {
            require(pos < s.length) { "Unexpected end" }
            if (s[pos] == '(') {
                pos++
                val value = expression()
                require(pos < s.length && s[pos] == ')') { "Missing )" }
                pos++
                return value
            }
            val start = pos
            while (pos < s.length && (s[pos].isDigit() || s[pos] == '.')) pos++
            require(pos > start) { "Number expected at $start" }
            return s.substring(start, pos).toDouble()
        }
    }
}
