package com.example.eyesofterapp.data.cache

import com.example.eyesofterapp.domain.model.Optica
import com.russhwolf.settings.Settings
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object DiskOpticasCache {
    private const val KEY_OPTICAS_JSON = "cached_opticas_json"
    private val settings: Settings by lazy { Settings() }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun guardar(list: List<Optica>) {
        if (list.isNotEmpty()) {
            try {
                val jsonString = json.encodeToString(list)
                settings.putString(KEY_OPTICAS_JSON, jsonString)
            } catch (e: Exception) {
                println("Error al guardar caché de ópticas en disco: ${e.message}")
            }
        }
    }

    fun obtener(): List<Optica>? {
        return try {
            val jsonString = settings.getStringOrNull(KEY_OPTICAS_JSON)
            if (!jsonString.isNullOrBlank()) {
                json.decodeFromString<List<Optica>>(jsonString)
            } else {
                null
            }
        } catch (e: Exception) {
            println("Error al leer caché de ópticas de disco: ${e.message}")
            null
        }
    }
}
