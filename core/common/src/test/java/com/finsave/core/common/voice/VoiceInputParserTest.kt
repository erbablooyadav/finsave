package com.finsave.core.common.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VoiceInputParserTest {

    @Test
    fun `parse extracts amount and merchant from rupees for phrase`() {
        val result = VoiceInputParser.parse("200 rupees for Zomato")

        assertEquals(20000L, result.amountPaise)
        assertEquals("Zomato", result.merchant)
    }

    @Test
    fun `parse extracts amount and merchant from spent at phrase`() {
        val result = VoiceInputParser.parse("spent 1500 at Swiggy")

        assertEquals(150000L, result.amountPaise)
        assertEquals("Swiggy", result.merchant)
    }

    @Test
    fun `parse extracts compact amount merchant phrase`() {
        val result = VoiceInputParser.parse("500 Uber")

        assertEquals(50000L, result.amountPaise)
        assertEquals("Uber", result.merchant)
    }

    @Test
    fun `parse returns amount only when merchant is absent`() {
        val result = VoiceInputParser.parse("1000")

        assertEquals(100000L, result.amountPaise)
        assertNull(result.merchant)
    }

    @Test
    fun `parse preserves decimal paise precision`() {
        val result = VoiceInputParser.parse("paid 250.50 to Amazon")

        assertEquals(25050L, result.amountPaise)
        assertEquals("Amazon", result.merchant)
    }

    @Test
    fun `parse returns null fields for blank input`() {
        val result = VoiceInputParser.parse("")

        assertNull(result.amountPaise)
        assertNull(result.merchant)
    }
}
