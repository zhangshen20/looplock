package com.histopgambling.looplock.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.histopgambling.looplock.domain.CONSENT_VERSION
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.loopLockPreferences by preferencesDataStore(name = "looplock_settings")

class ConsentStore(private val context: Context) {
    private val acceptedVersion = intPreferencesKey("accepted_disclosure_version")

    val isCurrentDisclosureAccepted: Flow<Boolean> = context.loopLockPreferences.data.map { preferences ->
        preferences[acceptedVersion] == CONSENT_VERSION
    }

    suspend fun acceptCurrentDisclosure() {
        context.loopLockPreferences.edit { preferences ->
            preferences[acceptedVersion] = CONSENT_VERSION
        }
    }
}

