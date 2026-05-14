package com.finsave.core.common.prefs

import android.content.Context
import com.finsave.core.common.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wraps [android.content.SharedPreferences] with typed accessors for booleans, ints, and strings.
 *
 * Annotated with [@Singleton] so Hilt provides a single instance across the app.
 * The [ApplicationContext] qualifier ensures no Activity context leaks.
 */
@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)

    // ── Boolean ────────────────────────────────────────────────────

    fun getBoolean(key: String, default: Boolean = false): Boolean =
        prefs.getBoolean(key, default)

    fun setBoolean(key: String, value: Boolean) =
        prefs.edit().putBoolean(key, value).apply()

    // ── Int ────────────────────────────────────────────────────────

    fun getInt(key: String, default: Int = 0): Int =
        prefs.getInt(key, default)

    fun setInt(key: String, value: Int) =
        prefs.edit().putInt(key, value).apply()

    fun getLong(key: String, default: Long = 0L): Long =
        prefs.getLong(key, default)

    fun setLong(key: String, value: Long) =
        prefs.edit().putLong(key, value).apply()

    // ── String ─────────────────────────────────────────────────────

    fun getString(key: String, default: String = ""): String =
        prefs.getString(key, default) ?: default

    fun setString(key: String, value: String) =
        prefs.edit().putString(key, value).apply()

    // ── Unseen SMS Count (for bottom nav badge) ─────────────────────

    fun getUnseenSmsCount(): Int =
        prefs.getInt(Constants.PREFS_UNSEEN_SMS_COUNT, 0)

    fun setUnseenSmsCount(count: Int) =
        prefs.edit().putInt(Constants.PREFS_UNSEEN_SMS_COUNT, count).apply()

    fun incrementUnseenSmsCount(delta: Int = 1) {
        val current = getUnseenSmsCount()
        setUnseenSmsCount(current + delta)
    }

    // ── Flow Accessors ──────────────────────────────────────────────

    fun getBooleanFlow(key: String, default: Boolean = false): kotlinx.coroutines.flow.Flow<Boolean> =
        kotlinx.coroutines.flow.flow {
            while (true) {
                emit(getBoolean(key, default))
                kotlinx.coroutines.delay(1000) // Simple polling fallback for shared prefs
            }
        }

    fun getIntFlow(key: String, default: Int = 0): kotlinx.coroutines.flow.Flow<Int> =
        kotlinx.coroutines.flow.flow {
            while (true) {
                emit(getInt(key, default))
                kotlinx.coroutines.delay(1000)
            }
        }
}
