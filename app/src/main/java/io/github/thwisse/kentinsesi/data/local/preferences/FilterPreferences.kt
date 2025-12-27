package io.github.thwisse.kentinsesi.data.local.preferences

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.thwisse.kentinsesi.data.model.FilterCriteria
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private val Context.filterDataStore by preferencesDataStore(name = "filter_prefs")

class FilterPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val LAST_APPLIED_PRESET_ID = stringPreferencesKey("last_applied_preset_id")

    private val EXAMPLE_PRESET_CREATED = booleanPreferencesKey("example_preset_created")

    private val LAST_DISTRICTS = stringSetPreferencesKey("last_filter_districts")
    private val LAST_CATEGORIES = stringSetPreferencesKey("last_filter_categories")
    private val LAST_STATUSES = stringSetPreferencesKey("last_filter_statuses")

    val lastAppliedPresetId: Flow<String?> = context.filterDataStore.data
        .map { prefs: Preferences -> prefs[LAST_APPLIED_PRESET_ID] }

    val lastCriteria: Flow<FilterCriteria?> = context.filterDataStore.data
        .map { prefs: Preferences ->
            val districts = prefs[LAST_DISTRICTS]
            val categories = prefs[LAST_CATEGORIES]
            val statuses = prefs[LAST_STATUSES]

            if (districts == null && categories == null && statuses == null) {
                null
            } else {
                FilterCriteria(
                    districts = districts?.toList().orEmpty(),
                    categories = categories?.toList().orEmpty(),
                    statuses = statuses?.toList().orEmpty()
                )
            }

        }

    val examplePresetCreated: Flow<Boolean> = context.filterDataStore.data
        .map { prefs: Preferences -> prefs[EXAMPLE_PRESET_CREATED] ?: false }

    suspend fun setLastAppliedPresetId(id: String?) {
        context.filterDataStore.edit { prefs ->
            if (id.isNullOrBlank()) {
                prefs.remove(LAST_APPLIED_PRESET_ID)
            } else {
                prefs[LAST_APPLIED_PRESET_ID] = id
            }
        }
    }

    suspend fun setExamplePresetCreated(created: Boolean) {
        context.filterDataStore.edit { prefs ->
            prefs[EXAMPLE_PRESET_CREATED] = created
        }
    }

    suspend fun setLastCriteria(criteria: FilterCriteria?) {
        context.filterDataStore.edit { prefs ->
            if (criteria == null) {
                prefs.remove(LAST_DISTRICTS)
                prefs.remove(LAST_CATEGORIES)
                prefs.remove(LAST_STATUSES)
            } else {
                prefs[LAST_DISTRICTS] = criteria.districts.toSet()
                prefs[LAST_CATEGORIES] = criteria.categories.toSet()
                prefs[LAST_STATUSES] = criteria.statuses.toSet()
            }
        }
    }
}
