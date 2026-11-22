package com.example.eyesofterapp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import com.example.eyesofterapp.plataforma.ArchivoElegido
import com.example.eyesofterapp.plataforma.Telefono
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Cámara, archivos, vibración y sonido en Android.
 *
 * - Cámara: se abre la app de cámara del teléfono (ACTION_IMAGE_CAPTURE). Así no
 *   hace falta pedir el permiso CAMERA: la foto la toma la app de cámara y la
 *   deja en un archivo temporal que le prestamos con FileProvider.
 * - Archivos: el selector del sistema (fotos o PDF), tampoco pide permisos.
 * - Las fotos se reducen a 1600 px y JPEG 80 % antes de subirlas (M-RC-05):
 *   una foto de 4 MB queda en unos 300 KB.
 *
 * Se crea como campo de MainActivity: registerForActivityResult debe llamarse
 * antes de que la actividad arranque.
 */
class TelefonoAndroid(private val activity: ComponentActivity) : Telefono {

    private var esperaFoto: CompletableDeferred<Boolean>? = null
    private var esperaArchivo: CompletableDeferred<Uri?>? = null

    private val lanzadorFoto = activity.registerForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        esperaFoto?.complete(ok)
    }
    private val lanzadorArchivo = activity.registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        esperaArchivo?.complete(uri)
    }

    override suspend fun tomarFoto(): ArchivoElegido? {
        val carpeta = File(activity.cacheDir, "fotos").apply { mkdirs() }
        // Se borran las fotos de veces anteriores: ya se subieron o se descartaron.
        carpeta.listFiles()?.forEach { it.delete() }
        val archivo = File(carpeta, "foto_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(activity, "${activity.packageName}.archivos", archivo)

        val espera = CompletableDeferred<Boolean>().also { esperaFoto = it }
        lanzadorFoto.launch(uri)
        if (!espera.await() || !archivo.exists() || archivo.length() == 0L) return null
        return withContext(Dispatchers.IO) { comprimir(activity, uri, "receta.jpg") }
    }

    override suspend fun elegirArchivo(): ArchivoElegido? {
        val espera = CompletableDeferred<Uri?>().also { esperaArchivo = it }
        lanzadorArchivo.launch(arrayOf("image/*", "application/pdf"))
        val uri = espera.await() ?: return null
        return withContext(Dispatchers.IO) {
            val tipo = activity.contentResolver.getType(uri).orEmpty()
            if (tipo == "application/pdf") {
                activity.contentResolver.openInputStream(uri)?.use { ArchivoElegido(it.readBytes(), "comprobante.pdf", tipo) }
            } else {
                comprimir(activity, uri, "comprobante.jpg")
            }
        }
    }

    override fun vibrar(patron: LongArray) {
        val vibrador = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (activity.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            activity.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        if (!vibrador.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrador.vibrate(VibrationEffect.createWaveform(patron, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrador.vibrate(patron, -1)
        }
    }

    override fun sonar() {
        runCatching {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            RingtoneManager.getRingtone(activity, uri)?.play()
        }
    }

    override fun mantenerPantallaEncendida(activa: Boolean) {
        activity.runOnUiThread {
            if (activa) activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            else activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private companion object {
        const val LADO_MAXIMO = 1600

        /** Lee la imagen reducida, la endereza según la orientación de la cámara y la guarda en JPEG. */
        fun comprimir(context: Context, uri: Uri, nombre: String): ArchivoElegido? {
            val resolver = context.contentResolver
            val medidas = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, medidas) }
            if (medidas.outWidth <= 0) return null

            // inSampleSize: potencia de 2 que deja el lado mayor cerca de LADO_MAXIMO sin cargar la foto completa.
            var muestra = 1
            while (maxOf(medidas.outWidth, medidas.outHeight) / (muestra * 2) >= LADO_MAXIMO) muestra *= 2
            val bitmap = resolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = muestra })
            } ?: return null

            val giro = resolver.openInputStream(uri)?.use {
                when (ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } ?: 0f

            val escala = minOf(1f, LADO_MAXIMO.toFloat() / maxOf(bitmap.width, bitmap.height))
            val matriz = Matrix().apply { postScale(escala, escala); postRotate(giro) }
            val final = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matriz, true)

            val salida = ByteArrayOutputStream()
            final.compress(Bitmap.CompressFormat.JPEG, 80, salida)
            return ArchivoElegido(salida.toByteArray(), nombre, "image/jpeg")
        }
    }
}
