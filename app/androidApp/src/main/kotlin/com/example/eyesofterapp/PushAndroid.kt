package com.example.eyesofterapp

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.eyesofterapp.plataforma.Push
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** Mismo id que usa el servidor (CanalPushFcm.CANAL_ANDROID). */
const val CANAL_AVISOS = "eyesofter_avisos"
const val EXTRA_RUTA = "ruta"

fun crearCanalAvisos(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val canal = NotificationChannel(CANAL_AVISOS, "Avisos de EYESOFTER", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Resultados de la revisión visual, recordatorios y avisos de la escuela"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
    }
}

/**
 * Firebase Cloud Messaging (M-RI-02). Debe crearse antes de onStart (registra el lanzador del permiso).
 * Si el proyecto no tiene google-services.json, Firebase no se inicializa y [token] devuelve null.
 */
class PushAndroid(private val activity: ComponentActivity) : Push {
    override val plataforma = "ANDROID"

    private var respuesta: CompletableDeferred<Boolean>? = null
    private val pedirPermiso = activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        respuesta?.complete(ok)
    }

    override suspend fun token(): String? {
        if (FirebaseApp.getApps(activity).isEmpty()) return null
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            // Sin permiso igual se registra el token: si después lo concede, ya recibe avisos.
            val r = CompletableDeferred<Boolean>().also { respuesta = it }
            pedirPermiso.launch(Manifest.permission.POST_NOTIFICATIONS)
            r.await()
        }
        return suspendCancellableCoroutine { c ->
            FirebaseMessaging.getInstance().token
                .addOnSuccessListener { c.resume(it) }
                .addOnFailureListener { c.resume(null) }
        }
    }
}

/** Recibe las push con la app abierta y los tokens nuevos. */
class AvisosFirebase : FirebaseMessagingService() {

    override fun onMessageReceived(mensaje: RemoteMessage) {
        // Con la app cerrada, Android muestra la "notification" solo; aquí llega cuando está abierta.
        val titulo = mensaje.notification?.title ?: mensaje.data["titulo"] ?: "EYESOFTER"
        val texto = mensaje.notification?.body ?: mensaje.data["mensaje"] ?: return
        crearCanalAvisos(this)
        val abrir = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            mensaje.data["ruta"]?.let { putExtra(EXTRA_RUTA, it) }
        }
        val pendiente = PendingIntent.getActivity(this, mensaje.data["notificacionId"].hashCode(), abrir,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(this, CANAL_AVISOS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(titulo)
            .setContentText(texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendiente)
            .build()
        if (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(this).notify(mensaje.data["notificacionId"].hashCode(), n)
        }
    }

    /** El token se vuelve a registrar en el servidor la próxima vez que se abre la sesión. */
    override fun onNewToken(token: String) {
        getSharedPreferences("push", MODE_PRIVATE).edit().putString("token", token).apply()
    }
}
