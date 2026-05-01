package com.finsave.core.common.formatter

import org.junit.Assert.assertEquals
import org.junit.Test

class IndianNumberFormatterTest {

    @Test
    fun `format uses Indian grouping for rupee amounts`() {
        assertEquals("\u20B90", IndianNumberFormatter.format(paiseAmount = 0))
        assertEquals("\u20B91", IndianNumberFormatter.format(paiseAmount = 100))
        assertEquals("\u20B9500", IndianNumberFormatter.format(paiseAmount = 50000))
        assertEquals("\u20B91,000", IndianNumberFormatter.format(paiseAmount = 100000))
        assertEquals("\u20B91,00,000", IndianNumberFormatter.format(paiseAmount = 10000000))
        assertEquals("\u20B91,00,00,000", IndianNumberFormatter.format(paiseAmount = 1000000000))
    }

    @Test
    fun `format preserves sign for negative values`() {
        assertEquals("-\u20B9500", IndianNumberFormatter.format(paiseAmount = -50000))
    }

    @Test
    fun `format shows paise when requested`() {
        assertEquals(
            "\u20B91,234.56",
            IndianNumberFormatter.format(paiseAmount = 123456, showPaise = true)
        )
    }

    @Test
    fun `formatCompact renders short chart labels`() {
        assertEquals("\u20B9500", IndianNumberFormatter.formatCompact(paiseAmount = 50000))
        assertEquals("\u20B95.0K", IndianNumberFormatter.formatCompact(paiseAmount = 500000))
        assertEquals("\u20B91.0L", IndianNumberFormatter.formatCompact(paiseAmount = 10000000))
        assertEquals("\u20B91.0Cr", IndianNumberFormatter.formatCompact(paiseAmount = 1000000000))
    }

    @Test
    fun `formatForAccessibility expands Indian amount words`() {
        assertEquals(
            "Rupees 1 thousand 234 and 56 paise",
            IndianNumberFormatter.formatForAccessibility(paiseAmount = 123456)
        )
        assertEquals(
            "Rupees 1 lakh",
            IndianNumberFormatter.formatForAccessibility(paiseAmount = 10000000)
        )
        assertEquals(
            "Rupees zero",
            IndianNumberFormatter.formatForAccessibility(paiseAmount = 0)
        )
    }
}
