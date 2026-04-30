package com.finsave.core.common.upi

import io.kotest.property.Arb
import io.kotest.property.arbitrary.long
import io.kotest.property.arbitrary.string
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.map
import io.kotest.property.checkAll
import org.junit.Test
import java.net.URLDecoder
import java.util.Locale

/**
 * Property-based tests for UpiUriBuilder.
 *
 * These tests validate universal properties that must hold for all inputs,
 * ensuring UPI URI generation is correct, secure, and compliant with NPCI specs.
 */
class UpiUriBuilderPropertyTest {

    /**
     * **Property 16: UPI URI validity**
     *
     * FOR ALL valid VPAs and positive paise amounts,
     * `UpiUriBuilder.build` SHALL return a string starting with `upi://pay?pa=`
     * that can be parsed as a valid URI without throwing.
     *
     * This ensures all generated URIs are syntactically valid and can be
     * consumed by UPI apps without errors.
     *
     * **Validates: Requirements 15.2**
     */
    @Test
    fun `property 16 - UPI URI validity`() = runTest {
        checkAll(100, validVpaArb(), Arb.long(1L..10_000_000L)) { vpa, amountPaise ->
            val uri = UpiUriBuilder.build(
                payeeVpa = vpa,
                payeeName = "Test User",
                amountPaise = amountPaise,
                transactionNote = "Test transaction"
            )

            // Assert URI is not null
            assert(uri != null) {
                "UpiUriBuilder.build returned null for valid VPA '$vpa' and amount $amountPaise"
            }

            // Assert URI starts with correct scheme and host
            assert(uri!!.startsWith("upi://pay?pa=")) {
                "URI does not start with 'upi://pay?pa=': $uri"
            }

            // Parse URI manually to validate structure
            val uriParts = uri.split("?", limit = 2)
            assert(uriParts.size == 2) {
                "URI does not have query parameters: $uri"
            }

            val schemeAndHost = uriParts[0]
            assert(schemeAndHost == "upi://pay") {
                "URI scheme and host incorrect: expected 'upi://pay', got '$schemeAndHost'"
            }

            // Parse query parameters
            val params = parseQueryParams(uriParts[1])
            
            // Verify required parameters exist
            assert(params.containsKey("pa")) {
                "URI missing 'pa' parameter: $uri"
            }
            assert(params.containsKey("pn")) {
                "URI missing 'pn' parameter: $uri"
            }
            assert(params.containsKey("am")) {
                "URI missing 'am' parameter: $uri"
            }
            assert(params.containsKey("cu")) {
                "URI missing 'cu' parameter: $uri"
            }

            // Verify VPA matches
            val decodedVpa = URLDecoder.decode(params["pa"], "UTF-8")
            assert(decodedVpa == vpa) {
                "Parsed VPA does not match: expected '$vpa', got '$decodedVpa'"
            }

            // Verify currency is INR
            assert(params["cu"] == "INR") {
                "Currency is not INR: ${params["cu"]}"
            }
        }
    }

    /**
     * **Property 17: UPI amount precision**
     *
     * FOR ALL paise amounts P,
     * the `am=` parameter in the URI SHALL equal `String.format(Locale.ROOT, "%.2f", P / 100.0)`.
     *
     * This ensures amounts are always formatted with exactly 2 decimal places
     * using locale-independent formatting, as required by the NPCI UPI spec.
     *
     * **Validates: Requirements 15.2, 15.4**
     */
    @Test
    fun `property 17 - UPI amount precision`() = runTest {
        checkAll(100, Arb.long(1L..10_000_000L)) { amountPaise ->
            val uri = UpiUriBuilder.build(
                payeeVpa = "test@upi",
                payeeName = "Test User",
                amountPaise = amountPaise,
                transactionNote = "Test"
            )

            assert(uri != null) {
                "UpiUriBuilder.build returned null for amount $amountPaise"
            }

            // Parse query parameters
            val queryString = uri!!.substringAfter("?")
            val params = parseQueryParams(queryString)
            val amountParam = params["am"]

            // Expected amount format
            val expectedAmount = String.format(Locale.ROOT, "%.2f", amountPaise / 100.0)

            assert(amountParam == expectedAmount) {
                "Amount parameter mismatch for $amountPaise paise: " +
                "expected '$expectedAmount', got '$amountParam' in URI: $uri"
            }

            // Verify amount has exactly 2 decimal places
            assert(amountParam?.matches(Regex("\\d+\\.\\d{2}")) == true) {
                "Amount parameter does not have exactly 2 decimal places: '$amountParam'"
            }
        }
    }

    /**
     * **Property 18: UPI note truncation**
     *
     * FOR ALL notes longer than 50 characters,
     * the `tn=` parameter (URL-decoded) SHALL contain at most 50 characters.
     *
     * This ensures compliance with the NPCI UPI spec which limits
     * transaction notes to 50 characters.
     *
     * **Validates: Requirements 15.4**
     */
    @Test
    fun `property 18 - UPI note truncation`() = runTest {
        checkAll(100, Arb.string(51..200)) { longNote ->
            val uri = UpiUriBuilder.build(
                payeeVpa = "test@upi",
                payeeName = "Test User",
                amountPaise = 100_00L,
                transactionNote = longNote
            )

            assert(uri != null) {
                "UpiUriBuilder.build returned null for note: $longNote"
            }

            // Parse query parameters
            val queryString = uri!!.substringAfter("?")
            val params = parseQueryParams(queryString)
            val encodedNote = params["tn"]

            // Note should be present
            assert(encodedNote != null) {
                "Transaction note parameter is missing in URI: $uri"
            }

            // Decode the note
            val noteParam = URLDecoder.decode(encodedNote!!, "UTF-8")

            // Note should be truncated to 50 chars
            assert(noteParam.length <= 50) {
                "Transaction note exceeds 50 characters: length=${noteParam.length}, " +
                "note='$noteParam', original length=${longNote.length}"
            }

            // Verify the note is a prefix of the original (truncated correctly)
            assert(longNote.startsWith(noteParam)) {
                "Truncated note is not a prefix of original: " +
                "original='$longNote', truncated='$noteParam'"
            }
        }
    }

    /**
     * Arbitrary generator for valid UPI VPAs.
     *
     * Generates VPAs matching the pattern: [alphanumeric.-]+@[alpha][alphanumeric]*
     * Examples: test@upi, user.name@gpay, user-123@paytm
     */
    private fun validVpaArb(): Arb<String> {
        val handles = listOf("upi", "gpay", "paytm", "ybl", "okaxis", "okhdfcbank", "apl")
        
        return Arb.string(3..20)
            .filter { it.matches(Regex("[a-zA-Z0-9.\\-]+")) }
            .map { username ->
                "$username@${handles.random()}"
            }
    }

    /**
     * Parse query parameters from a URI query string.
     * Example: "pa=test@upi&pn=Test&am=100.00" -> Map("pa" to "test@upi", "pn" to "Test", "am" to "100.00")
     */
    private fun parseQueryParams(queryString: String): Map<String, String> {
        return queryString.split("&")
            .mapNotNull { param ->
                val parts = param.split("=", limit = 2)
                if (parts.size == 2) {
                    parts[0] to parts[1]
                } else {
                    null
                }
            }
            .toMap()
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
