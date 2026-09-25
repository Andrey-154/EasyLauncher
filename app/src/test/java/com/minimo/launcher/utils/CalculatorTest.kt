package com.minimo.launcher.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CalculatorTest {
    @Test
    fun evaluatesExpressions() {
        assertEquals("500", Calculator.evaluate("125*4"))
        assertEquals("14", Calculator.evaluate("2+3*4"))
        assertEquals("20", Calculator.evaluate("(2+3)*4"))
        assertEquals("2.5", Calculator.evaluate("5/2"))
        assertEquals("2.5", Calculator.evaluate("5 : 2"))
        assertEquals("30", Calculator.evaluate("200*15%"))
        assertEquals("1024", Calculator.evaluate("2^10"))
        assertEquals("-6", Calculator.evaluate("-2*3"))
        assertEquals("3.3", Calculator.evaluate("1,1*3"))
        assertEquals("0.3", Calculator.evaluate("0.1+0.2"))
        assertEquals("0.333333333333", Calculator.evaluate("1/3"))
    }

    @Test
    fun ignoresNonMath() {
        assertNull(Calculator.evaluate("telegram"))
        assertNull(Calculator.evaluate("2"))
        assertNull(Calculator.evaluate("7zip"))
        assertNull(Calculator.evaluate("5/0"))
        assertNull(Calculator.evaluate("2+"))
        assertNull(Calculator.evaluate("(2+3"))
        assertNull(Calculator.evaluate(""))
    }
}
