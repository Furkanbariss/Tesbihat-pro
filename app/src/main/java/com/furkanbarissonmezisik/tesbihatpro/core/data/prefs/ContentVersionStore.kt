package com.furkanbarissonmezisik.tesbihatpro.core.data.prefs

import android.content.Context
import android.util.Log
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.first

/** Veritabanına yüklenmiş hazır içeriğin sürümünü tutar. */
interface ContentVersionStore {
    suspend fun read(): Int
    suspend fun write(version: Int)
}

private const val TAG = "ContentVersionStore"

private val Context.contentDataStore by preferencesDataStore(
    name = "content",
    corruptionHandler = ReplaceFileCorruptionHandler { exception ->
        Log.e(TAG, "İçerik sürümü dosyası bozuk, sıfırlanıyor", exception)
        emptyPreferences()
    },
)

class DataStoreContentVersionStore(private val context: Context) : ContentVersionStore {
    private val versionKey = intPreferencesKey("content_version")

    override suspend fun read(): Int = try {
        context.contentDataStore.data.first()[versionKey] ?: 0
    } catch (exception: IOException) {
        Log.e(TAG, "İçerik sürümü okunamadı", exception)
        0
    }

    override suspend fun write(version: Int) {
        try {
            context.contentDataStore.edit { it[versionKey] = version }
        } catch (exception: IOException) {
            Log.e(TAG, "İçerik sürümü yazılamadı", exception)
        }
    }
}
