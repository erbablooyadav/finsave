package com.finsave.core.common.voice

import java.math.BigDecimal
import java.math.RoundingMode

data class VoiceInputResult(
    val amountPaise: Long?,
    val merchant: String?
)

object VoiceInputParser {
    private val amountRegex = Regex("""\d+(?:\.\d{1,2})?""")
    private val keywordRegex = Regex(
        pattern = """\b(?:rupees?|rs\.?|inr|spent|paid|for|at|to|on)\b""",
        option = RegexOption.IGNORE_CASE
    )
    private val whitespaceRegex = Regex("""\s+""")

    fun parse(input: String): VoiceInputResult {
        val trimmed = input.trim()
        if (trimmed.isBlank()) {
            return VoiceInputResult(amountPaise = null, merchant = null)
        }

        val amountMatch = amountRegex.find(trimmed)
        val amountPaise = amountMatch?.value?.toPaise()
        val merchant = if (amountMatch != null) {
            trimmed
                .replaceFirst(amountRegex, " ")
                .replace(keywordRegex, " ")
                .replace(Regex("""[,:;]"""), " ")
                .replace(whitespaceRegex, " ")
                .trim()
                .ifBlank { null }
        } else {
            null
        }

        return VoiceInputResult(
            amountPaise = amountPaise,
            merchant = merchant
        )
    }

    private fun String.toPaise(): Long? {
        return try {
            BigDecimal(this)
                .multiply(BigDecimal("100"))
                .setScale(0, RoundingMode.HALF_UP)
                .toLong()
        } catch (e: NumberFormatException) {
            null
        } catch (e: ArithmeticException) {
            null
        }
    }
}
