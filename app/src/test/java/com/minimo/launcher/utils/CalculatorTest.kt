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
        assertEquals("2.5", Calculator.evaluate("5÷2"))
        assertEquals("1024", Calculator.evaluate("2^10"))
        assertEquals("-6", Calculator.evaluate("-2*3"))
        assertEquals("3.3", Calculator.evaluate("1,1*3"))
        assertEquals("0.3", Calculator.evaluate("0.1+0.2"))
        assertEquals("0.333333333333", Calculator.evaluate("1/3"))
        assertEquals("7", Calculator.evaluate("1 + 2 * 3"))
    }

    @Test
    fun unaryMinusBindsWeakerThanPower() {
        assertEquals("-4", Calculator.evaluate("-2^2"))
        assertEquals("4", Calculator.evaluate("(-2)^2"))
        assertEquals("0.5", Calculator.evaluate("2^-1"))
    }

    @Test
    fun percentWorksLikePhoneCalculators() {
        assertEquals("30", Calculator.evaluate("200*15%"))
        assertEquals("110", Calculator.evaluate("100+10%"))
        assertEquals("90", Calculator.evaluate("100-10%"))
        assertEquals("0.1", Calculator.evaluate("10%"))
        assertEquals("200", Calculator.evaluate("20/10%"))
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

    @Test
    fun ignoresDatesTimesAndPhoneNumbers() {
        assertNull(Calculator.evaluate("12/05/2026"))
        assertNull(Calculator.evaluate("12.05.2026"))
        assertNull(Calculator.evaluate("2:30"))
        assertNull(Calculator.evaluate("+7 999 123-45-67"))
    }

    @Test
    fun deepNestingDoesNotCrash() {
        assertNull(Calculator.evaluate("(".repeat(5000) + "1" + ")".repeat(5000)))
        assertNull(Calculator.evaluate("(".repeat(40) + "1+1" + ")".repeat(40)))
        assertEquals("2", Calculator.evaluate("((((1+1))))"))
    }
}
