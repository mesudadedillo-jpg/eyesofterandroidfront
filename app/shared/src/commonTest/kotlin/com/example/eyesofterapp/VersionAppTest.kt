package com.example.eyesofterapp

import com.example.eyesofterapp.data.network.leerVersionPublicada
import com.example.eyesofterapp.data.network.requiereActualizar
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VersionAppTest {
    private val publicada = leerVersionPublicada("""{"versionCode":142,"versionName":"1.42","apk":"https://x/eyesofter.apk","extra":1}""")

    @Test fun leeElJsonDelRelease() {
        assertEquals(142, publicada?.versionCode)
        assertEquals("1.42", publicada?.versionName)
        assertNull(leerVersionPublicada("<html>no es json</html>"))
    }

    @Test fun pideActualizarSoloSiHayUnaMasNueva() {
        assertTrue(requiereActualizar(141, publicada))
        assertFalse(requiereActualizar(142, publicada))
        assertFalse(requiereActualizar(150, publicada))
    }

    @Test fun sinRedOCompilacionDeDesarrolloDejaEntrar() {
        assertFalse(requiereActualizar(141, null))
        assertFalse(requiereActualizar(1, publicada))
    }
}
