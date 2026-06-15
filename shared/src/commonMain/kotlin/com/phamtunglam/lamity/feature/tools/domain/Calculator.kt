package com.phamtunglam.lamity.feature.tools.domain

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
                    '+' -> {
                        pos++
                        value += parseTerm()
                    }

                    '-' -> {
                        pos++
                        value -= parseTerm()
                    }

                    else -> {
                        return value
                    }
                }
            }
        }

        private fun parseTerm(): Double {
            var value = parseFactor()
            while (true) {
                skipWs()
                when (peek()) {
                    '*' -> {
                        pos++
                        value *= parseFactor()
                    }

                    '/' -> {
                        pos++
                        value /= parseFactor()
                    }

                    '%' -> {
                        pos++
                        value %= parseFactor()
                    }

                    else -> {
                        return value
                    }
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
                '-' -> {
                    pos++
                    -parseUnary()
                }

                '+' -> {
                    pos++
                    parseUnary()
                }

                else -> {
                    parsePrimary()
                }
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

                c.isDigit() || c == '.' -> {
                    parseNumber()
                }

                c.isLetter() -> {
                    parseIdentifier()
                }

                else -> {
                    fail("unexpected character '$c'")
                }
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
            UNARY_FUNCTIONS[name]?.let { fn ->
                if (args.size != 1) fail("$name expects 1 argument")
                return fn(args[0])
            }
            BINARY_FUNCTIONS[name]?.let { fn ->
                if (args.size != 2) fail("$name expects 2 arguments")
                return fn(args[0], args[1])
            }
            fail("unknown function '$name'")
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

        private fun skipWs() {
            while (peek() == ' ' || peek() == '\t') pos++
        }

        private fun fail(message: String): Nothing = throw IllegalArgumentException(message)

        private companion object {
            val UNARY_FUNCTIONS: Map<String, (Double) -> Double> =
                mapOf(
                    "sin" to { x -> sin(x) },
                    "cos" to { x -> cos(x) },
                    "tan" to { x -> tan(x) },
                    "asin" to { x -> asin(x) },
                    "acos" to { x -> acos(x) },
                    "atan" to { x -> atan(x) },
                    "sqrt" to { x -> sqrt(x) },
                    "abs" to { x -> abs(x) },
                    "ln" to { x -> ln(x) },
                    "log" to { x -> log10(x) },
                    "exp" to { x -> exp(x) },
                    "floor" to { x -> floor(x) },
                    "ceil" to { x -> ceil(x) },
                    "round" to { x -> round(x) },
                )

            val BINARY_FUNCTIONS: Map<String, (Double, Double) -> Double> =
                mapOf(
                    "min" to { a, b -> minOf(a, b) },
                    "max" to { a, b -> maxOf(a, b) },
                    "pow" to { a, b -> a.pow(b) },
                )
        }
    }
}
