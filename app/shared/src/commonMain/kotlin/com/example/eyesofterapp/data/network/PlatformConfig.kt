package com.example.eyesofterapp.data.network

// ---------------------------------------------------------------------------
// ÚNICO LUGAR donde se define a qué servidor se conecta la app.
// Lo usan HttpClientFactory (todas las peticiones), AuthApi, OpticasApi y
// los mensajes de error de AuthViewModel.
//
// Producción (Railway) — es el valor normal:
//     const val BASE_URL = "https://eyesofterandroid-production.up.railway.app"
//
// Servidor local en el EMULADOR de Android (backend corriendo con ./gradlew run):
//     const val BASE_URL = "http://10.0.2.2:8080"
//   10.0.2.2 es una dirección especial del emulador que significa
//   "la computadora donde corre el emulador". "localhost" NO sirve aquí,
//   porque dentro del emulador localhost es el propio teléfono virtual.
//   Funciona con http (sin "s") porque el AndroidManifest tiene
//   android:usesCleartextTraffic="true".
//
// Servidor local en un TELÉFONO FÍSICO: usa la IP de tu computadora en la
// misma red Wi-Fi (la ves con "ipconfig"), por ejemplo "http://192.168.1.50:8080".
//
// Después de cambiarlo, vuelve a compilar e instalar la app.
// ---------------------------------------------------------------------------

const val BASE_URL = "https://eyesofterandroid-production.up.railway.app"
