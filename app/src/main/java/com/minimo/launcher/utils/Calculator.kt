package com.minimo.launcher.utils

import java.math.BigDecimal
import java.math.MathContext
import kotlin.math.pow

/**
 * Tiny calculator for the app drawer search: numbers, + - * / ^, parentheses and percent.
 * Percent works like on phone calculators: "200*15%" = 30, "100+10%" = 110, "100-10%" = 90.
 * Returns null when the text is not a math expression (app names, dates, times, phone numbers).
 */
object Calculator {
    private const val MAX_LENGTH = 100
    private const val MAX_DEPTH = 20

    private val allowedChars = Regex("""^[\d\s.,+\-*/×÷^()%]+$""")
    private val hasOperator = Regex("""[\d)]\s*[+\-*/×÷^%]""")

    // Things that look like math but are not: 12/05/2026, 12.05.2026, +7 999 123-45-67
    private val dateLike = Regex("""^\d{1,4}([./-])\d{1,2}\1\d{1,4}$""")
    private val numbersSeparatedBySpace = Regex("""\d\s+\d""")

    fun evaluate(input: String): String? {
        val text = input.trim()
        if (text.isEmpty() || text.length > MAX_LENGTH) return null
        if (!allowedChars.matches(text) || !hasOperator.containsMatchIn(text)) return null
        if (dateLike.matches(text) || numbersSeparatedBySpace.containsMatchIn(text)) return null

        return try {
            val parser = Parser(
                text.replace(',', '.')
                    .replace('×', '*')
                    .replace('÷', '/')
                    .replace(" ", "")
            )
            val result = parser.parse()
            if (result.isNaN() || result.isInfinite()) null else format(result)
        } catch (_: Exception) {
            null
        } catch (_: StackOverflowError) {
            null
        }
    }

    private fun format(value: Double): String {
        val rounded = BigDecimal(value).round(MathContext(12)).stripTrailingZeros()
        return if (rounded.scale() <= 0) rounded.toBigInteger().toString() else rounded.toPlainString()
    }

    /**
     * Recursive descent, lowest to highest precedence:
     * expression (+ -) -> term (* /) -> unary (sign) -> power (^, right-assoc) -> percent -> primary.
     * Unary minus binds weaker than ^, so -2^2 = -4 like on Google/iOS calculators.
     */
    private class Parser(private val s: String) {
        private var pos = 0
        private var depth = 0

        /** Set when the last parsed term was a bare "N%" (for "100+10%" = 110). */
        private var lastTermIsPercent = false

        fun parse(): Double {
            val value = expression()
            require(pos == s.length) { "Unexpected character at $pos" }
            return value
        }

        private fun expression(): Double {
            var value = term()
            while (pos < s.length) {
                val op = s[pos]
                if (op != '+' && op != '-') return value
                pos++
                val right = term()
                // "a + b%" means a plus b percent of a
                val amount = if (lastTermIsPercent) value * right else right
                value = if (op == '+') value + amount else value - amount
            }
            return value
        }

        private fun term(): Double {
            var percentFactor = false
            var value = unary().also { percentFactor = lastFactorIsPercent }
            var single = true
            while (pos < s.length) {
                when (s[pos]) {
                    '*' -> {
                        pos++
                        value *= unary()
                    }

                    '/' -> {
                        pos++
                        val divisor = unary()
                        require(divisor != 0.0) { "Division by zero" }
                        value /= divisor
                    }

                    else -> break
                }
                single = false
            }
            lastTermIsPercent = single && percentFactor
            return value
        }

        private var lastFactorIsPercent = false

        private fun unary(): Double {
            if (pos < s.length && (s[pos] == '-' || s[pos] == '+')) {
                val negative = s[pos] == '-'
                pos++
                val value = unary()
                return if (negative) -value else value
            }
            return power()
        }

        private fun power(): Double {
            val base = percent()
            if (pos < s.length && s[pos] == '^') {
                pos++
                lastFactorIsPercent = false
                return base.pow(unary())
            }
            return base
        }

        private fun percent(): Double {
            var value = primary()
            lastFactorIsPercent = false
            while (pos < s.length && s[pos] == '%') {
                pos++
                value /= 100.0
                lastFactorIsPercent = true
            }
            return value
        }

        private fun primary(): Double {
            require(pos < s.length) { "Unexpected end" }
            if (s[pos] == '(') {
                require(++depth <= MAX_DEPTH) { "Too many parentheses" }
                pos++
                val value = expression()
                require(pos < s.length && s[pos] == ')') { "Missing )" }
                pos++
                depth--
                return value
            }
            val start = pos
            while (pos < s.length && (s[pos].isDigit() || s[pos] == '.')) pos++
            require(pos > start) { "Number expected at $start" }
            return s.substring(start, pos).toDouble()
        }
    }
}
