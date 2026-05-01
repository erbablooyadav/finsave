package com.finsave.domain.usecase.sms

import com.finsave.domain.usecase.sms.extractors.AmountExtractor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class AmountExtractorTest {

    private lateinit var amountExtractor: AmountExtractor

    @Before
    fun setup() {
        amountExtractor = AmountExtractor()
    }

    @Test
    fun `extractAmount converts whole rupees to paise`() {
        assertEquals(50000L, amountExtractor.extractAmount("500"))
        assertEquals(50000L, amountExtractor.extractAmount("500.00"))
    }

    @Test
    fun `extractAmount removes Indian and international comma grouping`() {
        assertEquals(123400L, amountExtractor.extractAmount("1,234"))
        assertEquals(12345600L, amountExtractor.extractAmount("1,23,456"))
        assertEquals(100000000L, amountExtractor.extractAmount("10,00,000"))
        assertEquals(1000000000L, amountExtractor.extractAmount("1,00,00,000"))
    }

    @Test
    fun `extractAmount preserves paise precision for decimal values`() {
        assertEquals(123450L, amountExtractor.extractAmount("1234.50"))
        assertEquals(50L, amountExtractor.extractAmount("0.50"))
        assertEquals(1L, amountExtractor.extractAmount("0.01"))
    }

    @Test
    fun `extractAmount truncates decimals beyond two places`() {
        assertEquals(1234L, amountExtractor.extractAmount("12.345"))
    }

    @Test
    fun `extractAmount rejects blank and malformed values`() {
        assertNull(amountExtractor.extractAmount(""))
        assertNull(amountExtractor.extractAmount("abc"))
        assertNull(amountExtractor.extractAmount("."))
    }

    @Test
    fun `extractAmount removes all commas before parsing`() {
        assertEquals(12300L, amountExtractor.extractAmount("1,2,3"))
    }

    @Test
    fun `extractAmount handles valid large numbers`() {
        assertEquals(123456789000L, amountExtractor.extractAmount("1234567890"))
    }
}
