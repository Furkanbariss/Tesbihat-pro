package com.furkanbarissonmezisik.tesbihatpro.core.data.prefs

import android.content.Context
import android.util.Log
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.furkanbarissonmezisik.tesbihatpro.core.domain.model.CounterState
import java.io.IOException
import kotlinx.coroutines.flow.first

interface SessionStore {
    suspend fun read(): CounterState?
    suspend fun write(state: CounterState)
}

private const val TAG = "SessionStore"

private val Context.sessionDataStore by preferencesDataStore(
    name = "session",
    corruptionHandler = ReplaceFileCorruptionHandler { exception ->
        Log.e(TAG, "Oturum dosyası bozuk, sıfırlanıyor", exception)
        emptyPreferences()
    },
)

class DataStoreSessionStore(private val context: Context) : SessionStore {

    override suspend fun read(): CounterState? {
        val preferences = try {
            context.sessionDataStore.data.first()
        } catch (exception: IOException) {
            Log.e(TAG, "Oturum okunamadı", exception)
            return null
        }
        val steps = preferences[Keys.STEPS] ?: return null
        val state = SessionCodec.decode(
            steps = steps,
            stepIndex = preferences[Keys.STEP_INDEX] ?: 0,
            count = preferences[Keys.COUNT] ?: 0,
            completed = preferences[Keys.COMPLETED] ?: false,
        )
        if (state == null) Log.e(TAG, "Oturum kaydı geçersiz: $steps")
        return state
    }

    override suspend fun write(state: CounterState) {
        try {
            context.sessionDataStore.edit { preferences ->
                preferences[Keys.STEPS] = SessionCodec.encodeSteps(state.steps)
                preferences[Keys.STEP_INDEX] = state.stepIndex
                preferences[Keys.COUNT] = state.count
                preferences[Keys.COMPLETED] = state.completed
                preferences[Keys.UPDATED_AT] = System.currentTimeMillis()
            }
        } catch (exception: IOException) {
            Log.e(TAG, "Oturum yazılamadı", exception)
        }
    }

    private object Keys {
        val STEPS = stringPreferencesKey("steps")
        val STEP_INDEX = intPreferencesKey("step_index")
        val COUNT = intPreferencesKey("count")
        val COMPLETED = booleanPreferencesKey("completed")
        val UPDATED_AT = longPreferencesKey("updated_at")
    }
}
