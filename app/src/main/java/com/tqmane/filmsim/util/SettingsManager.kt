package com.tqmane.filmsim.util

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.tqmane.filmsim.ui.Preset
import org.json.JSONArray
import org.json.JSONObject

/**
 * Type-safe wrapper around (Encrypted)SharedPreferences.
 *
 * On API 23+ an AES-256 master key protects persisted values.
 * On older devices a plain [SharedPreferences] file is used as a fallback.
 *
 * First launch transparently migrates data from the legacy unencrypted
 * "filmsim_settings" file so that existing users keep their preferences.
 */
class SettingsManager(context: Context) {

    companion object {
        private const val TAG = "SettingsManager"
        private const val LEGACY_PREFS = "filmsim_settings"
        private const val ENCRYPTED_PREFS = "filmsim_settings_enc"
        private const val KEY_MIGRATED = "_migrated"
        private const val KEY_PRESETS = "presets_json"
        private const val MAX_PRESETS = 20
    }

    private val prefs: SharedPreferences = createPreferences(context)

    // ─── Type-safe properties ───────────────────────────

    var savePath: String
        get() = prefs.getString("save_path", "Pictures/FilmSims") ?: "Pictures/FilmSims"
        set(value) = prefs.edit().putString("save_path", value).apply()

    var saveQuality: Int
        get() = prefs.getInt("save_quality", 100)
        set(value) = prefs.edit().putInt("save_quality", value).apply()

    var lastIntensity: Float
        get() = prefs.getFloat("last_intensity", 1f).coerceIn(0f, 1f)
        set(value) = prefs.edit().putFloat("last_intensity", value).apply()

    var lastGrainEnabled: Boolean
        get() = prefs.getBoolean("last_grain_enabled", false)
        set(value) = prefs.edit().putBoolean("last_grain_enabled", value).apply()

    var lastGrainIntensity: Float
        get() = prefs.getFloat("last_grain_intensity", 0.5f).coerceIn(0f, 1f)
        set(value) = prefs.edit().putFloat("last_grain_intensity", value).apply()

    var lastGrainStyle: String
        get() = prefs.getString("last_grain_style", "Xiaomi") ?: "Xiaomi"
        set(value) = prefs.edit().putString("last_grain_style", value).apply()

    // Basic adjustment persistence
    var lastExposure: Float
        get() = prefs.getFloat("last_exposure", 0f).coerceIn(-2f, 2f)
        set(value) = prefs.edit().putFloat("last_exposure", value).apply()

    var lastContrast: Float
        get() = prefs.getFloat("last_contrast", 0f).coerceIn(-1f, 1f)
        set(value) = prefs.edit().putFloat("last_contrast", value).apply()

    var lastHighlights: Float
        get() = prefs.getFloat("last_highlights", 0f).coerceIn(-1f, 1f)
        set(value) = prefs.edit().putFloat("last_highlights", value).apply()

    var lastShadows: Float
        get() = prefs.getFloat("last_shadows", 0f).coerceIn(-1f, 1f)
        set(value) = prefs.edit().putFloat("last_shadows", value).apply()

    var lastColorTemp: Float
        get() = prefs.getFloat("last_color_temp", 0f).coerceIn(-1f, 1f)
        set(value) = prefs.edit().putFloat("last_color_temp", value).apply()

    // ─── Preset CRUD ────────────────────────────────────

    fun loadPresets(): List<Preset> {
        val json = prefs.getString(KEY_PRESETS, null) ?: return emptyList()
        return try {
            val array = JSONArray(json)
            (0 until array.length()).map { i -> array.getJSONObject(i).toPreset() }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse presets", e)
            emptyList()
        }
    }

    fun savePreset(preset: Preset): Boolean {
        val list = loadPresets().toMutableList()
        if (list.size >= MAX_PRESETS) return false
        list.add(preset)
        persistPresets(list)
        return true
    }

    fun deletePreset(id: String) {
        val list = loadPresets().filter { it.id != id }
        persistPresets(list)
    }

    private fun persistPresets(presets: List<Preset>) {
        val array = JSONArray()
        presets.forEach { array.put(it.toJson()) }
        prefs.edit().putString(KEY_PRESETS, array.toString()).apply()
    }

    private fun Preset.toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("lutPath", lutPath ?: "")
        put("intensity", intensity.toDouble())
        put("grainEnabled", grainEnabled)
        put("grainIntensity", grainIntensity.toDouble())
        put("grainStyle", grainStyle)
        put("exposure", exposure.toDouble())
        put("contrast", contrast.toDouble())
        put("highlights", highlights.toDouble())
        put("shadows", shadows.toDouble())
        put("colorTemp", colorTemp.toDouble())
        put("watermarkStyleName", watermarkStyleName)
        put("watermarkDeviceName", watermarkDeviceName)
        put("watermarkTimeText", watermarkTimeText)
        put("watermarkLocationText", watermarkLocationText)
        put("watermarkLensInfo", watermarkLensInfo)
    }

    private fun JSONObject.toPreset(): Preset = Preset(
        id = getString("id"),
        name = getString("name"),
        lutPath = getString("lutPath").ifEmpty { null },
        intensity = optDouble("intensity", 1.0).toFloat(),
        grainEnabled = optBoolean("grainEnabled", false),
        grainIntensity = optDouble("grainIntensity", 0.5).toFloat(),
        grainStyle = optString("grainStyle", "Xiaomi"),
        exposure = optDouble("exposure", 0.0).toFloat(),
        contrast = optDouble("contrast", 0.0).toFloat(),
        highlights = optDouble("highlights", 0.0).toFloat(),
        shadows = optDouble("shadows", 0.0).toFloat(),
        colorTemp = optDouble("colorTemp", 0.0).toFloat(),
        watermarkStyleName = optString("watermarkStyleName", "NONE"),
        watermarkDeviceName = optString("watermarkDeviceName", ""),
        watermarkTimeText = optString("watermarkTimeText", ""),
        watermarkLocationText = optString("watermarkLocationText", ""),
        watermarkLensInfo = optString("watermarkLensInfo", "")
    )

    // ─── Internal helpers ───────────────────────────────

    private fun createPreferences(context: Context): SharedPreferences {
        val encrypted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val masterKey = MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()

                EncryptedSharedPreferences.create(
                    context,
                    ENCRYPTED_PREFS,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create encrypted prefs, falling back", e)
                null
            }
        } else null

        val target = encrypted ?: context.getSharedPreferences(LEGACY_PREFS, Context.MODE_PRIVATE)

        // Migrate legacy prefs on first launch after upgrade
        if (encrypted != null && !target.getBoolean(KEY_MIGRATED, false)) {
            migrateFromLegacy(context, target)
        }

        return target
    }

    private fun migrateFromLegacy(context: Context, destination: SharedPreferences) {
        val legacy = context.getSharedPreferences(LEGACY_PREFS, Context.MODE_PRIVATE)
        if (legacy.all.isEmpty()) {
            destination.edit().putBoolean(KEY_MIGRATED, true).apply()
            return
        }

        val editor = destination.edit()
        for ((key, value) in legacy.all) {
            when (value) {
                is String -> editor.putString(key, value)
                is Int -> editor.putInt(key, value)
                is Float -> editor.putFloat(key, value)
                is Boolean -> editor.putBoolean(key, value)
                is Long -> editor.putLong(key, value)
                is Set<*> -> @Suppress("UNCHECKED_CAST") editor.putStringSet(key, value as Set<String>)
            }
        }
        editor.putBoolean(KEY_MIGRATED, true)
        editor.apply()

        Log.d(TAG, "Migrated ${legacy.all.size} entries from legacy prefs")
    }
}
