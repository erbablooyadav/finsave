package com.finsave.app.sync

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

import com.finsave.data.local.sms.BankPatternConfigProvider
import com.finsave.data.local.sms.SmsInboxReader
import com.finsave.domain.model.Transaction
import com.finsave.domain.repository.TransactionRepository
import com.finsave.domain.repository.AccountRepository
import com.finsave.domain.usecase.sms.SmsParserEngine
import com.finsave.domain.model.TransactionType
import kotlinx.coroutines.flow.firstOrNull
import java.time.Instant
import java.time.ZoneId

@HiltWorker
class SmsSyncWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val smsInboxReader: SmsInboxReader,
    private val smsParserEngine: SmsParserEngine,
    private val bankPatternConfigProvider: BankPatternConfigProvider,
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        // Guard: check READ_SMS permission at runtime before touching the content provider.
        // The user may have denied it during onboarding or revoked it later.
        if (ContextCompat.checkSelfPermission(appContext, Manifest.permission.READ_SMS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "READ_SMS permission not granted — skipping SMS sync")
            return Result.success() // Not a failure; just nothing to do
        }

        return try {
            val config = bankPatternConfigProvider.getConfig()

            val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
            val sinceTimestamp = inputData.getLong("sinceTimestamp", sevenDaysAgo)

            val rawMessages = smsInboxReader.readBankSms(
                sinceTimestamp = sinceTimestamp,
                limit = 1000
            )
            Log.d(TAG, "Read ${rawMessages.size} bank SMS from inbox")

            val defaultAccount = accountRepository.getDefaultAccount().firstOrNull()
            val fallbackAccount = defaultAccount ?: accountRepository.getAllAccounts().firstOrNull()?.firstOrNull()
            
            if (fallbackAccount == null) {
                Log.w(TAG, "No account exists. Cannot import SMS transactions.")
                return Result.success()
            }
            val accountId = fallbackAccount.id

            var imported = 0
            var skippedDuplicate = 0
            var skippedNoParse = 0

            for (sms in rawMessages) {
                val parsedTx = smsParserEngine.parse(
                    smsId = sms.id,
                    sender = sms.sender,
                    body = sms.body,
                    timestamp = sms.timestamp,
                    config = config
                )

                if (parsedTx == null) {
                    skippedNoParse++
                    continue
                }

                val isDuplicate = transactionRepository.isSmsDuplicate(parsedTx.duplicateHash)
                if (isDuplicate) {
                    skippedDuplicate++
                    continue
                }

                val tx = Transaction(
                    id = 0,
                    amountPaise = parsedTx.amountPaise,
                    type = if (parsedTx.type == com.finsave.domain.model.sms.TransactionType.DEBIT)
                        TransactionType.DEBIT else TransactionType.CREDIT,
                    categoryId = 0L,
                    accountId = accountId,
                    merchantName = parsedTx.merchant,
                    note = "Auto-imported from SMS",
                    date = Instant.ofEpochMilli(parsedTx.timestamp)
                        .atZone(ZoneId.systemDefault()).toLocalDate(),
                    upiId = if (parsedTx.merchant.contains("@")) parsedTx.merchant else null,
                    isAutoImported = true,
                    smsHash = parsedTx.duplicateHash
                )
                transactionRepository.insertTransaction(tx)
                imported++
            }

            Log.i(TAG, "SMS sync done — imported=$imported, duplicates=$skippedDuplicate, no-parse=$skippedNoParse")
            Result.success()
        } catch (e: SecurityException) {
            // READ_SMS was revoked mid-execution
            Log.e(TAG, "SecurityException reading SMS — permission revoked?", e)
            Result.success() // Don't retry; permission won't fix itself
        } catch (e: Exception) {
            Log.e(TAG, "SMS sync failed", e)
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "SmsSyncWorker"
        private const val TAG = "SmsSyncWorker"
    }
}
