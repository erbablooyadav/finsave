package com.finsave.core.common.share

import android.graphics.Bitmap
import android.graphics.Canvas
import com.finsave.core.common.Constants
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import com.finsave.core.common.formatter.IndianNumberFormatter

/**
 * Data class holding all inputs for the Spend DNA personality card.
 * Contains only aggregate/anonymized data — no PII (OWASP A09).
 */
data class SpendDnaData(
    val topCategory: String,
    val topCategoryEmoji: String,
    val topCategoryAmount: Long,   // paise
    val totalSpent: Long,          // paise
    val savingsPercent: Int,       // 0–100
    val personalityType: PersonalityType,
    val month: String              // e.g. "May 2026"
)

enum class PersonalityType(
    val displayName: String,
    val emoji: String,
    val gradientStart: Int,
    val gradientEnd: Int
) {
    WEEKEND_WARRIOR("Weekend Warrior", "⚡", 0xFF6A11CB.toInt(), 0xFF2575FC.toInt()),
    FOOD_LOVER("Food Lover", "🍕", 0xFFFF512F.toInt(), 0xFFDD2476.toInt()),
    SMART_SAVER("Smart Saver", "💎", 0xFF11998E.toInt(), 0xFF38EF7D.toInt()),
    SUBSCRIPTION_HOARDER("Subscription Hoarder", "📱", 0xFFF857A6.toInt(), 0xFFFF5858.toInt()),
    DAILY_DRIVER("Daily Driver", "🚗", 0xFF4568DC.toInt(), 0xFFB06AB3.toInt())
}

/**
 * Generates a 1080×1920 shareable "Spend DNA" personality card bitmap
 * using only android.graphics.Canvas — no third-party libraries.
 *
 * Security: Only aggregate data is rendered. No account numbers, card numbers,
 * or identifiable transaction details are included.
 */
object SpendDnaGenerator {

    private const val WIDTH = 1080
    private const val HEIGHT = 1920

    fun generate(data: SpendDnaData): Bitmap {
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // ── Background gradient ────────────────────────────────────
        val bgPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, HEIGHT.toFloat(),
                data.personalityType.gradientStart,
                data.personalityType.gradientEnd,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(), bgPaint)

        // ── Reusable white text paint ──────────────────────────────
        val whitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            textAlign = Paint.Align.CENTER
        }

        val centerX = WIDTH / 2f

        // ── Personality emoji + name (top) ─────────────────────────
        whitePaint.textSize = 120f
        canvas.drawText(data.personalityType.emoji, centerX, 300f, whitePaint)

        whitePaint.textSize = 72f
        whitePaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(data.personalityType.displayName, centerX, 420f, whitePaint)

        // ── Top category subtitle ──────────────────────────────────
        whitePaint.textSize = 36f
        whitePaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val categoryText = "${data.topCategoryEmoji} ${IndianNumberFormatter.format(data.topCategoryAmount)} on ${data.topCategory}"
        canvas.drawText(categoryText, centerX, 540f, whitePaint)
        canvas.drawText("this month", centerX, 590f, whitePaint)

        // ── Divider line ───────────────────────────────────────────
        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x40FFFFFF  // semi-transparent white
            strokeWidth = 2f
        }
        canvas.drawLine(WIDTH * 0.2f, 680f, WIDTH * 0.8f, 680f, dividerPaint)

        // ── Stats row ──────────────────────────────────────────────
        whitePaint.textSize = 40f
        whitePaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val spentFormatted = IndianNumberFormatter.format(data.totalSpent, showPaise = false)
        val statsText = "Spent: $spentFormatted  •  Saved: ${data.savingsPercent}%"
        canvas.drawText(statsText, centerX, 780f, whitePaint)

        // ── Large personality emoji (center decorative) ────────────
        whitePaint.textSize = 200f
        whitePaint.alpha = 40  // Very faint watermark emoji
        canvas.drawText(data.personalityType.emoji, centerX, 1150f, whitePaint)
        whitePaint.alpha = 255

        // ── Month label ────────────────────────────────────────────
        whitePaint.textSize = 32f
        whitePaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText(data.month, centerX, HEIGHT - 280f, whitePaint)

        // ── FinSave watermark ──────────────────────────────────────
        whitePaint.textSize = 22f
        whitePaint.alpha = 180
        canvas.drawText(
            "Made with FinSave — your private finance tracker",
            centerX,
            HEIGHT - 180f,
            whitePaint
        )

        // ── Play Store link ────────────────────────────────────────
        whitePaint.textSize = 18f
        whitePaint.alpha = 140
        canvas.drawText(
            Constants.PLAY_STORE_LINK,
            centerX,
            HEIGHT - 120f,
            whitePaint
        )

        return bitmap
    }
}
