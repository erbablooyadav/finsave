package com.finsave.domain.usecase.sms

import com.finsave.domain.model.sms.BankConfig
import com.finsave.domain.model.sms.BankPatternConfig
import com.finsave.domain.model.sms.SmsPattern
import com.finsave.domain.model.sms.TransactionType
import com.finsave.domain.usecase.sms.extractors.AmountExtractor
import com.finsave.domain.usecase.sms.extractors.AutoCategoriser
import com.finsave.domain.usecase.sms.extractors.MerchantExtractor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class SmsParserEngineTest {

    private lateinit var engine: SmsParserEngine
    private lateinit var testConfig: BankPatternConfig

    @Before
    fun setup() {
        engine = SmsParserEngine(
            amountExtractor = AmountExtractor(),
            merchantExtractor = MerchantExtractor(),
            autoCategoriser = AutoCategoriser()
        )
        
        testConfig = BankPatternConfig(
            version = 1,
            lastUpdated = "2025",
            banks = listOf(
                BankConfig(
                    bankName = "HDFC Bank",
                    senderIds = listOf("HDFCBK"),
                    patterns = listOf(
                        SmsPattern(
                            type = TransactionType.DEBIT,
                            regex = "(?:debited|spent).*?(?:Rs\\.?|INR|₹)\\s*([\\d,]+\\.?\\d*)",
                            amountGroup = 1,
                            merchantRegex = "(?:to|at)\\s+([A-Za-z0-9\\s\\.\\-]+?)(?:\\s+on|\\s+Ref|\\.|$)",
                            upiRegex = "(?:VPA|UPI)\\s*[:\\-]?\\s*([a-zA-Z0-9.\\-]+@[a-zA-Z]+)"
                        )
                    )
                ),
                BankConfig(
                    bankName = "ICICI Bank",
                    senderIds = listOf("ICICIB"),
                    patterns = listOf(
                        SmsPattern(
                            type = TransactionType.DEBIT,
                            regex = "(?:debited|spent).*?(?:Rs\\.?|INR|₹)\\s*([\\d,]+\\.?\\d*)",
                            amountGroup = 1,
                            merchantRegex = "(?:to|at)\\s+([A-Za-z0-9\\s\\.\\-]+?)(?:\\s+on|\\s+Ref|\\.|$)",
                            upiRegex = "(?:VPA|UPI)\\s*[:\\-]?\\s*([a-zA-Z0-9.\\-]+@[a-zA-Z]+)"
                        )
                    )
                )
            ),
            autoCategoryMappings = mapOf(
                "zomato" to "Food & Dining",
                "uber" to "Transport"
            )
        )
    }

    @Test
    fun `parse valid HDFC debit SMS`() {
        val sms = "Debited Rs.500.00 from account **1234 to Zomato on 28-04-26."
        val result = engine.parse(
            smsId = "1",
            sender = "AD-HDFCBK",
            body = sms,
            timestamp = 1600000000L,
            config = testConfig
        )
        
        assertNotNull(result)
        assertEquals("HDFC Bank", result?.bankName)
        assertEquals(50000L, result?.amountPaise)
        assertEquals(TransactionType.DEBIT, result?.type)
        assertEquals("Zomato", result?.merchant)
        assertEquals("Food & Dining", result?.category)
    }
    
    @Test
    fun `parse valid ICICI debit with explicit merchant`() {
        val sms = "Acct XX123 debited for Rs 1,250.50. Info: spent at UBER INDIA SYSTEMS. Ref No 1234."
        val result = engine.parse(
            smsId = "2",
            sender = "DM-ICICIB",
            body = sms,
            timestamp = 1600000000L,
            config = testConfig
        )
        
        assertNotNull(result)
        assertEquals("ICICI Bank", result?.bankName)
        assertEquals(125050L, result?.amountPaise)
        assertEquals(TransactionType.DEBIT, result?.type)
        assertEquals("UBER INDIA SYSTEMS", result?.merchant)
        assertEquals("Transport", result?.category)
    }
    
    @Test
    fun `ignore SMS from unknown sender`() {
        val sms = "Debited Rs.500.00 from account **1234 to VPA zomato@hdfcbank on 28-04-26."
        val result = engine.parse(
            smsId = "3",
            sender = "UNKNOWN",
            body = sms,
            timestamp = 1600000000L,
            config = testConfig
        )
        
        assertEquals(null, result)
    }

    @Test
    fun `extract merchant from VPA when explicit merchant is missing`() {
        val sms = "Debited Rs.500.00 from a/c **1234. UPI: zomato@hdfcbank."
        val result = engine.parse(
            smsId = "4",
            sender = "AD-HDFCBK",
            body = sms,
            timestamp = 1600000000L,
            config = testConfig
        )
        
        assertNotNull(result)
        assertEquals("Zomato", result?.merchant)
        assertEquals("Food & Dining", result?.category)
    }

    @Test
    fun `extract merchant from VPA phone number`() {
        val sms = "Debited Rs.500.00 from a/c **1234. UPI: 9876543210@ybl."
        val result = engine.parse("5", "AD-HDFCBK", sms, 1600000000L, testConfig)
        
        assertNotNull(result)
        assertEquals("UPI User (9876543210)", result?.merchant)
    }

    @Test
    fun `extract merchant from generic platform VPA`() {
        val sms = "Debited Rs.500.00 from a/c **1234. UPI: pay.tm.112@paytm."
        val result = engine.parse("6", "AD-HDFCBK", sms, 1600000000L, testConfig)
        
        assertNotNull(result)
        assertEquals("PayTM Merchant", result?.merchant)
    }
}
