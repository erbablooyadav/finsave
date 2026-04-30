package com.finsave.core.common.upi

/**
 * UPI Deep Link URI Builder — Constructs upi:// payment intent URIs.
 *
 * Spec: NPCI UPI Deep Linking Guidelines
 * URI Format: upi://pay?pa=<VPA>&pn=<Name>&am=<Amount>&cu=INR&tn=<Note>
 *
 * This builder is used by the Bill Splitter's "Settle Up" feature to launch
 * the user's UPI app (GPay, PhonePe, Paytm) with pre-filled payment details.
 */
object UpiUriBuilder {

    private const val UPI_SCHEME = "upi"
    private const val UPI_HOST = "pay"
    private const val CURRENCY = "INR"

    // UPI VPA regex: alphanumeric + dots + hyphens, @ followed by provider handle
    private val UPI_ID_REGEX = Regex("^[a-zA-Z0-9.\\-]+@[a-zA-Z][a-zA-Z0-9]*$")

    /**
     * Builds a UPI payment URI string.
     *
     * @param payeeVpa UPI Virtual Payment Address (e.g., "rahul@gpay")
     * @param payeeName Display name of the payee (e.g., "Rahul Sharma")
     * @param amountPaise Amount in paise (e.g., 125000 = ₹1,250.00)
     * @param transactionNote Optional note (e.g., "Goa Trip settlement")
     * @return UPI URI string, or null if inputs are invalid
     */
    fun build(
        payeeVpa: String,
        payeeName: String,
        amountPaise: Long,
        transactionNote: String? = null
    ): String? {
        // Validate VPA format
        if (!isValidUpiId(payeeVpa)) return null

        // Amount must be positive
        if (amountPaise <= 0) return null

        // Convert paise to rupees with 2 decimal places
        val amountRupees = String.format(
            java.util.Locale.ROOT,
            "%.2f",
            amountPaise / 100.0
        )

        val builder = StringBuilder()
        builder.append("$UPI_SCHEME://$UPI_HOST")
        builder.append("?pa=").append(encode(payeeVpa))
        builder.append("&pn=").append(encode(payeeName))
        builder.append("&am=").append(amountRupees)
        builder.append("&cu=").append(CURRENCY)

        if (!transactionNote.isNullOrBlank()) {
            // NPCI spec limits transaction note to 50 characters
            val truncatedNote = transactionNote.take(50)
            builder.append("&tn=").append(encode(truncatedNote))
        }

        return builder.toString()
    }

    /**
     * Validates a UPI ID format.
     *
     * Valid examples: rahul@gpay, priya.sharma@paytm, user-123@ybl
     * Invalid: @gpay, rahul@, rahul, "", null
     */
    fun isValidUpiId(upiId: String?): Boolean {
        if (upiId.isNullOrBlank()) return false
        if (upiId.length > 50) return false // Reasonable max length
        return UPI_ID_REGEX.matches(upiId)
    }

    /**
     * Extracts the UPI provider handle from a VPA.
     * Example: "rahul@gpay" → "gpay"
     */
    fun extractProvider(upiId: String): String? {
        if (!isValidUpiId(upiId)) return null
        return upiId.substringAfter("@")
    }

    /**
     * Returns display-friendly provider name.
     * "gpay" → "Google Pay", "paytm" → "Paytm", etc.
     */
    fun getProviderDisplayName(upiId: String): String {
        val handle = extractProvider(upiId) ?: return "UPI"
        return PROVIDER_NAMES[handle.lowercase()] ?: handle.replaceFirstChar { it.uppercase() }
    }

    /**
     * Simple URL encoding for UPI URI parameters.
     * Spaces → %20, special chars encoded.
     */
    private fun encode(value: String): String {
        return java.net.URLEncoder.encode(value, "UTF-8")
            .replace("+", "%20")
    }

    /**
     * Known UPI provider handles → display names.
     */
    private val PROVIDER_NAMES = mapOf(
        "gpay" to "Google Pay",
        "oksbi" to "Google Pay (SBI)",
        "okicici" to "Google Pay (ICICI)",
        "okaxis" to "Google Pay (Axis)",
        "okhdfcbank" to "Google Pay (HDFC)",
        "paytm" to "Paytm",
        "ybl" to "PhonePe",
        "ibl" to "PhonePe (ICICI)",
        "axl" to "PhonePe (Axis)",
        "apl" to "Amazon Pay",
        "rapl" to "Amazon Pay",
        "icici" to "iMobile Pay",
        "sbi" to "YONO SBI",
        "upi" to "BHIM",
        "axisbank" to "Axis Bank",
        "hdfcbank" to "HDFC Bank",
        "kotak" to "Kotak",
        "indus" to "IndusInd Bank",
        "boi" to "BOI",
        "pnb" to "PNB",
        "federal" to "Federal Bank",
        "citi" to "Citi",
        "rbl" to "RBL Bank",
        "freecharge" to "Freecharge",
        "mobikwik" to "MobiKwik",
        "slice" to "Slice",
        "jupiteraxis" to "Jupiter",
        "fi" to "Fi Money"
    )
}
