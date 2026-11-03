package com.example.faceswap

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Cliente del backend Flask (ver ../app_python/: `python app.py serve`).
 *
 * El swap real corre en el PC porque insightface/onnxruntime no existen
 * para Android. La app envía las 2 fotos a POST /swap y muestra el JPG.
 * Los modelos se descargan solos en el PC (~800 MB), nunca en el APK.
 */
object FaceSwapEngine {

    /** Carpeta privada donde la app descargaría modelos en runtime (futuro). */
    fun modelsDir(activity: Activity): File {
        val dir = File(activity.filesDir, "models")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /** Envía las fotos al backend y devuelve el JPG resultado. Lanza si falla. */
    @Throws(Exception::class)
    fun swapRemote(baseUrl: String, source: ByteArray, target: ByteArray): ByteArray {
        val boundary = "----faceswap" + System.currentTimeMillis()
        val url = URL(baseUrl.trimEnd('/') + "/swap")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 30_000
            readTimeout = 600_000 // el swap en CPU puede tardar minutos
            setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
        }
        try {
            conn.outputStream.use { out ->
                fun part(name: String, data: ByteArray) {
                    out.write("--$boundary\r\n".toByteArray())
                    out.write("Content-Disposition: form-data; name=\"$name\"; filename=\"$name.jpg\"\r\n".toByteArray())
                    out.write("Content-Type: image/jpeg\r\n\r\n".toByteArray())
                    out.write(data)
                    out.write("\r\n".toByteArray())
                }
                part("source", source)
                part("target", target)
                out.write("--$boundary--\r\n".toByteArray())
                out.flush()
            }
            val code = conn.responseCode
            val body = (if (code in 200..299) conn.inputStream else conn.errorStream)?.readBytes()
                ?: ByteArray(0)
            if (code !in 200..299) {
                val msg = try {
                    String(body, Charsets.UTF_8).substringAfter("\"error\":\"").substringBefore("\"")
                } catch (_: Exception) {
                    ""
                }
                throw Exception("Backend error $code: ${msg.ifEmpty { "revisa el servidor" }}")
            }
            return body
        } finally {
            conn.disconnect()
        }
    }
}

class MainActivity : Activity() {

    private lateinit var sourceView: ImageView
    private lateinit var targetView: ImageView
    private lateinit var resultView: ImageView
    private lateinit var backendUrlEdit: EditText
    private lateinit var statusText: TextView
    private lateinit var swapButton: Button

    private var sourceUri: Uri? = null
    private var targetUri: Uri? = null

    private val pickSourceCode = 2001
    private val pickTargetCode = 2002
    private val defaultBackend = "http://192.168.1.100:5000"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sourceView = findViewById(R.id.source_image)
        targetView = findViewById(R.id.target_image)
        resultView = findViewById(R.id.result_image)
        backendUrlEdit = findViewById(R.id.backend_url)
        statusText = findViewById(R.id.status)
        swapButton = findViewById(R.id.swap)

        backendUrlEdit.setText(prefs().getString("backend_url", defaultBackend))

        findViewById<Button>(R.id.pick_source).setOnClickListener { pickImage(pickSourceCode) }
        findViewById<Button>(R.id.pick_target).setOnClickListener { pickImage(pickTargetCode) }
        swapButton.setOnClickListener { doSwap() }

        statusText.text = "Elige foto fuente y destino, revisa la URL y pulsa Swap."
    }

    private fun prefs() = getSharedPreferences("faceswap", MODE_PRIVATE)

    private fun pickImage(code: Int) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
        }
        @Suppress("DEPRECATION") // Sin AndroidX a propósito: app mínima.
        startActivityForResult(intent, code)
    }

    @Deprecated("Sin AndroidX a propósito: app mínima")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        try {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: Exception) {
        }
        when (requestCode) {
            pickSourceCode -> {
                sourceUri = uri
                sourceView.setImageURI(uri)
                statusText.text = "Fuente: ${displayName(uri)}"
            }
            pickTargetCode -> {
                targetUri = uri
                targetView.setImageURI(uri)
                statusText.text = "Destino: ${displayName(uri)}"
            }
        }
    }

    private fun displayName(uri: Uri): String {
        try {
            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) return c.getString(0) ?: "imagen"
            }
        } catch (_: Exception) {
        }
        return "imagen"
    }

    private fun doSwap() {
        val src = sourceUri
        val dst = targetUri
        if (src == null || dst == null) {
            Toast.makeText(this, "Elige las dos fotos primero", Toast.LENGTH_SHORT).show()
            return
        }
        val base = backendUrlEdit.text.toString().trim()
        if (!base.startsWith("http")) {
            Toast.makeText(this, "URL del backend no válida (ej: http://192.168.1.100:5000)", Toast.LENGTH_LONG).show()
            return
        }
        prefs().edit().putString("backend_url", base).apply()
        swapButton.isEnabled = false
        statusText.text = "Enviando fotos al backend..."
        Thread {
            try {
                val srcBytes = readUri(src)
                val dstBytes = readUri(dst)
                runOnUiThread { statusText.text = "Procesando en el servidor (en CPU tarda minutos)..." }
                val result = FaceSwapEngine.swapRemote(base, srcBytes, dstBytes)
                val bmp = BitmapFactory.decodeByteArray(result, 0, result.size)
                    ?: throw Exception("El servidor no devolvió una imagen")
                runOnUiThread {
                    resultView.setImageBitmap(bmp)
                    statusText.text = "Listo. Resultado abajo."
                }
            } catch (e: Exception) {
                runOnUiThread {
                    statusText.text = "Error: ${e.message}"
                    Toast.makeText(this, "Falló: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } finally {
                runOnUiThread { swapButton.isEnabled = true }
            }
        }.start()
    }

    private fun readUri(uri: Uri): ByteArray {
        return contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw Exception("No se pudo leer la imagen elegida")
    }
}
