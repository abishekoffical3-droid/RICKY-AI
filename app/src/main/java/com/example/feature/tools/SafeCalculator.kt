package com.example.feature.tools

import kotlin.math.*

/**
 * Safe Mathematical Expression Evaluator.
 * Parses and computes arithmetic expressions without arbitrary code execution.
 */
object SafeCalculator {

    sealed interface CalcResult {
        data class Success(val value: Double, val formattedResult: String) : CalcResult
        data class Error(val message: String) : CalcResult
    }

    fun isMathExpression(input: String): Boolean {
        val trimmed = input.trim().lowercase()
        if (trimmed.isEmpty()) return false
        // Must contain at least one math operator or known function
        val hasOp = trimmed.any { it in "+-*/%^" } ||
                trimmed.startsWith("sqrt") ||
                trimmed.startsWith("sin") ||
                trimmed.startsWith("cos") ||
                trimmed.startsWith("log") ||
                trimmed.startsWith("abs")
        val validChars = trimmed.all {
            it.isDigit() || it in "+-*/%^()., \t\r\n" ||
                    it in "abcdefghijklmnopqrstuvwxyz"
        }
        return hasOp && validChars
    }

    fun evaluate(expression: String): CalcResult {
        return try {
            val sanitized = expression
                .replace("×", "*")
                .replace("÷", "/")
                .replace(" ", "")
            val parser = ExprParser(sanitized)
            val res = parser.parse()
            val formatted = if (res % 1.0 == 0.0) {
                res.toLong().toString()
            } else {
                "%.6f".format(res).trimEnd('0').trimEnd('.')
            }
            CalcResult.Success(res, formatted)
        } catch (e: Exception) {
            CalcResult.Error(e.message ?: "Invalid mathematical expression")
        }
    }

    private class ExprParser(private val str: String) {
        private var pos = -1
        private var ch = ' '

        private fun nextChar() {
            ch = if (++pos < str.length) str[pos] else '\u0000'
        }

        private fun eat(charToEat: Char): Boolean {
            while (ch == ' ') nextChar()
            if (ch == charToEat) {
                nextChar()
                return true
            }
            return false
        }

        fun parse(): Double {
            nextChar()
            val x = parseExpression()
            if (pos < str.length) throw IllegalArgumentException("Unexpected character: '$ch'")
            return x
        }

        private fun parseExpression(): Double {
            var x = parseTerm()
            while (true) {
                when {
                    eat('+') -> x += parseTerm()
                    eat('-') -> x -= parseTerm()
                    else -> return x
                }
            }
        }

        private fun parseTerm(): Double {
            var x = parseFactor()
            while (true) {
                when {
                    eat('*') -> x *= parseFactor()
                    eat('/') -> {
                        val divisor = parseFactor()
                        if (divisor == 0.0) throw ArithmeticException("Division by zero")
                        x /= divisor
                    }
                    eat('%') -> x %= parseFactor()
                    else -> return x
                }
            }
        }

        private fun parseFactor(): Double {
            if (eat('+')) return parseFactor()
            if (eat('-')) return -parseFactor()

            var x: Double
            val startPos = pos
            if (eat('(')) {
                x = parseExpression()
                if (!eat(')')) throw IllegalArgumentException("Missing closing parenthesis")
            } else if ((ch in '0'..'9') || ch == '.') {
                while ((ch in '0'..'9') || ch == '.') nextChar()
                x = str.substring(startPos, pos).toDouble()
            } else if (ch in 'a'..'z') {
                while (ch in 'a'..'z') nextChar()
                val func = str.substring(startPos, pos)
                if (!eat('(')) throw IllegalArgumentException("Expected '(' after function $func")
                val arg = parseExpression()
                if (!eat(')')) throw IllegalArgumentException("Missing ')' after argument to $func")
                x = when (func) {
                    "sqrt" -> {
                        if (arg < 0) throw ArithmeticException("Cannot take square root of negative number")
                        sqrt(arg)
                    }
                    "sin" -> sin(Math.toRadians(arg))
                    "cos" -> cos(Math.toRadians(arg))
                    "tan" -> tan(Math.toRadians(arg))
                    "log" -> {
                        if (arg <= 0) throw ArithmeticException("Logarithm of non-positive number")
                        log10(arg)
                    }
                    "ln" -> {
                        if (arg <= 0) throw ArithmeticException("Logarithm of non-positive number")
                        ln(arg)
                    }
                    "abs" -> abs(arg)
                    else -> throw IllegalArgumentException("Unknown function: $func")
                }
            } else {
                throw IllegalArgumentException("Unexpected token: '$ch'")
            }

            if (eat('^')) x = x.pow(parseFactor())

            return x
        }
    }
}
