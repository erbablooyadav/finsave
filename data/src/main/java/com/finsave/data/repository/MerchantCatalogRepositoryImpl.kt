package com.finsave.data.repository

import android.content.Context
import com.finsave.domain.repository.MerchantCatalogRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONArray
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MerchantCatalogRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : MerchantCatalogRepository {

    @Volatile
    private var cachedMerchants: List<String>? = null

    override suspend fun getBundledMerchantNames(): List<String> {
        cachedMerchants?.let { return it }

        return synchronized(this) {
            cachedMerchants ?: loadMerchants().also { cachedMerchants = it }
        }
    }

    private fun loadMerchants(): List<String> {
        val json = context.assets.open("merchant_names.json").use { input ->
            input.bufferedReader().use { it.readText() }
        }
        val array = JSONArray(json)
        return buildList {
            for (index in 0 until array.length()) {
                add(array.getString(index))
            }
        }
    }
}
