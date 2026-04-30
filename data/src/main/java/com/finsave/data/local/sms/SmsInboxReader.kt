package com.finsave.data.local.sms

import android.content.Context
import android.provider.Telephony
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Reads SMS from the device's inbox.
 * Uses a broad heuristic to identify institutional/transactional sender IDs
 * (e.g., "AD-HDFCBK", "UnionB") and strictly filters out personal 10-digit mobile numbers
 * to ensure we never process personal messages.
 */
class SmsInboxReader @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    data class RawSms(
        val id: String,
        val sender: String,
        val body: String,
        val timestamp: Long
    )

    /**
     * Reads the last [limit] SMS messages from the inbox that appear to be from institutions.
     * Runs on the IO dispatcher as it performs database I/O via ContentResolver.
     */
    suspend fun readBankSms(
        sinceTimestamp: Long = 0L,
        limit: Int = 500
    ): List<RawSms> = withContext(Dispatchers.IO) {
        val smsList = mutableListOf<RawSms>()
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE
        )

        val selection = "${Telephony.Sms.DATE} >= ?"
        val selectionArgs = arrayOf(sinceTimestamp.toString())

        // Only query the inbox. Note: LIMIT in the sort order string is not supported on
        // all Android versions, so we cap results in code instead.
        val cursor = context.contentResolver.query(
            Telephony.Sms.Inbox.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            "${Telephony.Sms.DATE} DESC"
        )

        cursor?.use {
            val idIndex = it.getColumnIndexOrThrow(Telephony.Sms._ID)
            val addressIndex = it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val bodyIndex = it.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val dateIndex = it.getColumnIndexOrThrow(Telephony.Sms.DATE)

            var count = 0
            while (it.moveToNext() && count < limit) {
                val address = it.getString(addressIndex) ?: ""
                
                // Privacy filter: Only process SMS if the sender appears to be an institution.
                // We strictly exclude any sender that is purely digits (like a 10-digit phone number)
                // and ensure the sender contains at least some letters.
                val isPersonalNumber = address.matches(Regex("^\\+?\\d{10,15}$"))
                val hasLetters = address.any { it.isLetter() }
                
                val isBankSms = !isPersonalNumber && hasLetters

                if (isBankSms) {
                    smsList.add(
                        RawSms(
                            id = it.getString(idIndex),
                            sender = address,
                            body = it.getString(bodyIndex) ?: "",
                            timestamp = it.getLong(dateIndex)
                        )
                    )
                    count++
                }
            }
        }
        return@withContext smsList
    }
}
