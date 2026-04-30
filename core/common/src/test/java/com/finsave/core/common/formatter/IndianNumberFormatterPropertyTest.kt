package com.finsave.core.common.formatter

import io.kotest.property.Arb
import io.kotest.property.arbitrary.long
import io.kotest.property.checkAll
import org.junit.Test

/**
 * Property-based tests for IndianNumberFormatter.
 *
 * These tests validate universal properties that must hold for all inputs,
 * complementing the example-based unit tests.
 */
class IndianNumberFormatterPropertyTest {

    /**
     * **Property 14: Indian number format round-trip**
     *
     * FOR ALL paise amounts P ≥ 0,
     * `parseToPaise(format(P, showPaise=true).removePrefix("₹"))` SHALL equal P.
     *
     * This ensures that formatting and parsing are inverse operations,
     * guaranteeing no data loss in round-trip conversions.
     *
     * **Validates: Requirements 14.1, 14.8**
     */
    @Test
    fun `property 14 - Indian number format round-trip`() = runTest {
        checkAll(100, Arb.long(0L..100_000_000_000L)) { paiseAmount ->
            // Format with paise display enabled
            val formatted = IndianNumberFormatter.format(
                paiseAmount = paiseAmount,
                useIndianSystem = true,
                showPaise = true,
                showSymbol = true
            )

            // Remove currency symbol and parse back
            val parsedBack = IndianNumberFormatter.parseToPaise(formatted.removePrefix("₹"))

            // Assert round-trip equality
            assert(parsedBack == paiseAmount) {
                "Round-trip failed for $paiseAmount: formatted='$formatted', parsed=$parsedBack"
            }
        }
    }

    /**
     * **Property 15: Indian grouping pattern**
     *
     * FOR ALL rupees ≥ 1,000,
     * the formatted string SHALL have a comma after the hundreds digit,
     * then groups of two digits (Indian numbering system).
     *
     * Pattern: X,XX,XXX (e.g., 1,23,456 not 123,456)
     *
     * **Validates: Requirements 14.1**
     */
    @Test
    fun `property 15 - Indian grouping pattern`() = runTest {
        checkAll(100, Arb.long(100_000L..100_000_000_000L)) { paiseAmount ->
            val formatted = IndianNumberFormatter.format(
                paiseAmount = paiseAmount,
                useIndianSystem = true,
                showPaise = false,
                showSymbol = true
            )

            // Remove currency symbol
            val numberPart = formatted.removePrefix("₹").replace(",", "")
            val rupees = paiseAmount / 100

            // Verify the number part matches the rupees value
            assert(numberPart.toLong() == rupees) {
                "Number mismatch for $paiseAmount: formatted='$formatted', expected rupees=$rupees"
            }

            // Verify Indian grouping pattern for amounts >= 1000
            if (rupees >= 1000) {
                val withCommas = formatted.removePrefix("₹")
                
                // Indian format: last 3 digits, then groups of 2
                // Example: 1,23,456 or 12,34,567 or 1,23,45,678
                
                // Count commas - should be (digits - 3) / 2 rounded up
                val digitCount = numberPart.length
                val expectedCommas = (digitCount - 3 + 1) / 2
                val actualCommas = withCommas.count { it == ',' }
                
                assert(actualCommas == expectedCommas) {
                    "Indian grouping pattern failed for $paiseAmount (rupees=$rupees): " +
                    "formatted='$formatted', expected $expectedCommas commas, got $actualCommas"
                }

                // Verify last group is 3 digits (before first comma from right)
                val parts = withCommas.split(",")
                if (parts.isNotEmpty()) {
                    val lastGroup = parts.last()
                    assert(lastGroup.length == 3) {
                        "Last group should be 3 digits for $paiseAmount: formatted='$formatted', last group='$lastGroup'"
                    }
                }

                // Verify middle groups are 2 digits (except the first group which can be 1 or 2)
                if (parts.size > 2) {
                    for (i in 1 until parts.size - 1) {
                        assert(parts[i].length == 2) {
                            "Middle group $i should be 2 digits for $paiseAmount: formatted='$formatted', group='${parts[i]}'"
                        }
                    }
                }
            }
        }
    }

    /**
     * Helper to run suspending tests.
     * Kotest property tests are not suspend by default in JUnit context.
     */
    private fun runTest(block: suspend () -> Unit) {
        kotlinx.coroutines.runBlocking {
            block()
        }
    }
}
