package com.finsave.data.local.sms

import android.content.Context
import com.finsave.domain.model.sms.BankConfig
import com.finsave.domain.model.sms.BankPatternConfig
import com.finsave.domain.model.sms.SmsPattern
import com.finsave.domain.model.sms.TransactionType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BankPatternConfigLoader @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * Loads and parses bank_patterns.json from assets.
     */
    suspend fun loadConfig(): BankPatternConfig = withContext(Dispatchers.IO) {
        val jsonString = context.assets.open("bank_patterns.json").use { inputStream ->
            InputStreamReader(inputStream).readText()
        }
        
        val jsonObject = JSONObject(jsonString)
        val version = jsonObject.getInt("version")
        val lastUpdated = jsonObject.getString("lastUpdated")
        
        val banksArray = jsonObject.getJSONArray("banks")
        val banks = mutableListOf<BankConfig>()
        
        for (i in 0 until banksArray.length()) {
            val bankObj = banksArray.getJSONObject(i)
            val bankName = bankObj.getString("bankName")
            
            val senderIdsArray = bankObj.getJSONArray("senderIds")
            val senderIds = mutableListOf<String>()
            for (j in 0 until senderIdsArray.length()) {
                senderIds.add(senderIdsArray.getString(j))
            }
            
            val patternsArray = bankObj.getJSONArray("patterns")
            val patterns = mutableListOf<SmsPattern>()
            for (k in 0 until patternsArray.length()) {
                val patternObj = patternsArray.getJSONObject(k)
                patterns.add(
                    SmsPattern(
                        type = TransactionType.valueOf(patternObj.getString("type")),
                        regex = patternObj.getString("regex"),
                        amountGroup = patternObj.optInt("amountGroup", 1),
                        merchantRegex = patternObj.optString("merchantRegex", "").takeIf { it.isNotEmpty() },
                        upiRegex = patternObj.optString("upiRegex", "").takeIf { it.isNotEmpty() }
                    )
                )
            }
            
            banks.add(BankConfig(bankName, senderIds, patterns))
        }
        
        val mappingsObj = jsonObject.getJSONObject("autoCategoryMappings")
        val mappings = mutableMapOf<String, String>()
        val keys = mappingsObj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            mappings[key] = mappingsObj.getString(key)
        }
        
        BankPatternConfig(version, lastUpdated, banks, mappings)
    }
}
