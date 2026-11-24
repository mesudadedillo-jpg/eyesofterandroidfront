package com.example.eyesofterapp.data.network

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Versión obligatoria de la app (Android).
 *
 * Cada vez que se publica un APK (workflow "Publicar APK", que corre en cada cambio a main),
 * junto al instalador se sube version.json al release más reciente de GitHub. Al abrir la
 * app se consulta y, si hay una versión más nueva que la instalada, la app no deja entrar
 * hasta actualizar.
 *
 * Si no hay internet o GitHub no responde, se deja entrar: la app debe funcionar sin red
 * y el servidor sigue validando cada petición.
 */
const val URL_VERSION_APP = "https://github.com/mesudadedillo-jpg/eyesofterandroidfront/releases/latest/download/version.json"

/** Los APK de desarrollo (compilados a mano) tienen versionCode 1: a esos no se les exige actualizar. */
const val VERSION_CODE_MINIMA_PUBLICADA = 100

@Serializable
data class VersionPublicada(
    val versionCode: Int,
    val versionName: String = "",
    /** Enlace directo al APK de esa versión. */
    val apk: String = "",
    val notas: String = "",
)

private val lector = Json { ignoreUnknownKeys = true; isLenient = true }

fun leerVersionPublicada(json: String): VersionPublicada? =
    runCatching { lector.decodeFromString(VersionPublicada.serializer(), json) }.getOrNull()

/** True si hay que actualizar antes de usar la app. */
fun requiereActualizar(instalada: Int, publicada: VersionPublicada?): Boolean =
    publicada != null && instalada >= VERSION_CODE_MINIMA_PUBLICADA && publicada.versionCode > instalada

class VersionApi(private val client: HttpClient, private val url: String = URL_VERSION_APP) {
    /** null si no se pudo consultar (sin red, GitHub caído...). */
    suspend fun ultima(): VersionPublicada? = runCatching {
        val r = client.get(url)
        if (r.status.isSuccess()) leerVersionPublicada(r.bodyAsText()) else null
    }.getOrNull()
}
