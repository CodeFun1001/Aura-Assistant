package com.kastack.auraassistant.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.kastack.auraassistant.domain.models.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserProfileDataStore @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private val KEY_NAME = stringPreferencesKey("profile_name")
        private val KEY_AGE = stringPreferencesKey("profile_age")
        private val KEY_PHONE = stringPreferencesKey("profile_phone")
        private val KEY_TRAITS = stringPreferencesKey("profile_traits")
        private val KEY_ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
    }

    val userProfile: Flow<UserProfile> = dataStore.data.map { prefs ->
        UserProfile(
            name = prefs[KEY_NAME] ?: "",
            age = prefs[KEY_AGE] ?: "",
            phone = prefs[KEY_PHONE] ?: "",
            traits = (prefs[KEY_TRAITS] ?: "")
                .split(",")
                .filter { it.isNotBlank() },
            isOnboardingComplete = prefs[KEY_ONBOARDING_COMPLETE] ?: false
        )
    }

    suspend fun save(profile: UserProfile) {
        dataStore.edit { prefs ->
            prefs[KEY_NAME] = profile.name
            prefs[KEY_AGE] = profile.age
            prefs[KEY_PHONE] = profile.phone
            prefs[KEY_TRAITS] = profile.traits.joinToString(",")
            prefs[KEY_ONBOARDING_COMPLETE] = profile.isOnboardingComplete
        }
    }
}