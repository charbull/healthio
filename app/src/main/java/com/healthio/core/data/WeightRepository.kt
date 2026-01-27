package com.healthio.core.data

import android.content.Context
import com.healthio.core.database.AppDatabase
import com.healthio.core.database.WeightLog
import kotlinx.coroutines.flow.Flow

class WeightRepository(context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val dao = db.weightDao()

    suspend fun logWeight(weight: WeightLog) {
        dao.insertWeight(weight)
    }

    suspend fun updateWeight(weight: WeightLog) {
        dao.updateWeight(weight)
    }

    fun getLatestWeight(): Flow<WeightLog?> {
        return dao.getLatestWeight()
    }

    fun getAllWeights(): Flow<List<WeightLog>> {
        return dao.getAllWeights()
    }

    suspend fun getImportedExternalIds(): List<String> {
        return dao.getImportedExternalIds()
    }

    suspend fun getWeightByExternalId(externalId: String): WeightLog? {
        return dao.getWeightByExternalId(externalId)
    }
}
