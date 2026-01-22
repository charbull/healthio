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

    fun getLatestWeight(): Flow<WeightLog?> {
        return dao.getLatestWeight()
    }

    suspend fun getImportedExternalIds(): List<String> {
        return dao.getImportedExternalIds()
    }
}
