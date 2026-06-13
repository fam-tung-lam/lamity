package com.phamtunglam.lamity.core.tools

import kotlin.math.E
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * Small recursive-descent evaluator for arithmetic expressions used by the
 * calculate tool. Supports + - * / % ^, parentheses, constants (pi, e) and
 * functions: sin cos tan asin acos atan sqrt abs ln log exp floor ceil round,
 * min(a,b) max(a,b) pow(a,b). Trigonometry uses radians.
 */
object Calculator {

    fun evaluate(expression: String): Double {
        val parser = Parser(expression)
        val value = parser.parseExpression()
        parser.expectEnd()
        return value
    }

    private class Parser(private val src: String) {
        private var pos = 0

        fun parseExpression(): Double {
            var value = parseTerm()
            while (true) {
                skipWs()
                when (peek()) {
                    '+' -> { pos++; value += parseTerm() }
                    '-' -> { pos++; value -= parseTerm() }
                    else -> return value
                }
            }
        }

        private fun parseTerm(): Double {
            var value = parseFactor()
            while (true) {
                skipWs()
                when (peek()) {
                    '*' -> { pos++; value *= parseFactor() }
                    '/' -> { pos++; value /= parseFactor() }
                    '%' -> { pos++; value %= parseFactor() }
                    else -> return value
                }
            }
        }

        private fun parseFactor(): Double {
            val base = parseUnary()
            skipWs()
            if (peek() == '^') {
                pos++
                return base.pow(parseFactor()) // right associative
            }
            return base
        }

        private fun parseUnary(): Double {
            skipWs()
            return when (peek()) {
                '-' -> { pos++; -parseUnary() }
                '+' -> { pos++; parseUnary() }
                else -> parsePrimary()
            }
        }

        private fun parsePrimary(): Double {
            skipWs()
            val c = peek() ?: fail("unexpected end of expression")
            return when {
                c == '(' -> {
                    pos++
                    val v = parseExpression()
                    expect(')')
                    v
                }
                c.isDigit() || c == '.' -> parseNumber()
                c.isLetter() -> parseIdentifier()
                else -> fail("unexpected character '$c'")
            }
        }

        private fun parseNumber(): Double {
            val start = pos
            while (peek()?.let { it.isDigit() || it == '.' } == true) pos++
            if (peek() == 'e' || peek() == 'E') {
                pos++
                if (peek() == '+' || peek() == '-') pos++
                while (peek()?.isDigit() == true) pos++
            }
            val text = src.substring(start, pos)
            return text.toDoubleOrNull() ?: fail("invalid number '$text'")
        }

        private fun parseIdentifier(): Double {
            val start = pos
            while (peek()?.let { it.isLetterOrDigit() || it == '_' } == true) pos++
            val name = src.substring(start, pos).lowercase()
            skipWs()
            if (peek() == '(') {
                pos++
                val args = mutableListOf(parseExpression())
                skipWs()
                while (peek() == ',') {
                    pos++
                    args.add(parseExpression())
                    skipWs()
                }
                expect(')')
                return applyFunction(name, args)
            }
            return when (name) {
                "pi" -> PI
                "e" -> E
                else -> fail("unknown constant '$name'")
            }
        }

        private fun applyFunction(name: String, args: List<Double>): Double {
            fun one(): Double {
                if (args.size != 1) fail("$name expects 1 argument")
                return args[0]
            }
            fun two(): Pair<Double, Double> {
                if (args.size != 2) fail("$name expects 2 arguments")
                return args[0] to args[1]
            }
            return when (name) {
                "sin" -> sin(one())
                "cos" -> cos(one())
                "tan" -> tan(one())
                "asin" -> asin(one())
                "acos" -> acos(one())
                "atan" -> atan(one())
                "sqrt" -> sqrt(one())
                "abs" -> abs(one())
                "ln" -> ln(one())
                "log" -> log10(one())
                "exp" -> exp(one())
                "floor" -> floor(one())
                "ceil" -> ceil(one())
                "round" -> round(one())
                "min" -> two().let { minOf(it.first, it.second) }
                "max" -> two().let { maxOf(it.first, it.second) }
                "pow" -> two().let { it.first.pow(it.second) }
                else -> fail("unknown function '$name'")
            }
        }

        fun expectEnd() {
            skipWs()
            if (pos < src.length) fail("unexpected trailing input '${src.substring(pos)}'")
        }

        private fun expect(c: Char) {
            skipWs()
            if (peek() != c) fail("expected '$c'")
            pos++
        }

        private fun peek(): Char? = src.getOrNull(pos)
        private fun skipWs() { while (peek() == ' ' || peek() == '\t') pos++ }
        private fun fail(message: String): Nothing = throw IllegalArgumentException(message)
    }
}
