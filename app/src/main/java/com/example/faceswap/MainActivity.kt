package com.example.faceswap

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import java.io.File

/**
 * Puente hacia el swap real (ver ../app_python/).
 *
 * Decisión: sin Chaquopy. Chaquopy no puede ejecutar insightface ni
 * onnxruntime en Android (no existen wheels de esos paquetes para
 * Android, y buffalo+inswapper suman ~800 MB). Integración prevista:
 *   1) Backend Flask: POST /swap a un PC (ver app_python/app.py serve).
 *   2) Reescritura nativa con ONNX Runtime para Android + OpenCV SDK.
 * Los modelos, en cualquier caso, se descargan en runtime a modelsDir().
 */
object FaceSwapEngine {

    /** Carpeta privada donde la app descargará los modelos en runtime. */
    fun modelsDir(activity: Activity): File {
        val dir = File(activity.filesDir, "models")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }
}

class MainActivity : Activity() {

    private lateinit var sourceView: ImageView
    private lateinit var targetView: ImageView
    private lateinit var statusText: TextView

    private var sourceUri: Uri? = null
    private var targetUri: Uri? = null

    private val pickSourceCode = 2001
    private val pickTargetCode = 2002

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sourceView = findViewById(R.id.source_image)
        targetView = findViewById(R.id.target_image)
        statusText = findViewById(R.id.status)

        findViewById<Button>(R.id.pick_source).setOnClickListener { pickImage(pickSourceCode) }
        findViewById<Button>(R.id.pick_target).setOnClickListener { pickImage(pickTargetCode) }
        findViewById<Button>(R.id.swap).setOnClickListener { doSwap() }

        statusText.text = "Elige foto fuente y foto destino."
    }

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
        if (sourceUri == null || targetUri == null) {
            Toast.makeText(this, "Elige las dos fotos primero", Toast.LENGTH_SHORT).show()
            return
        }
        // Placeholder: el swap real vive en app_python/ (buffalo + inswapper, CPU).
        statusText.text = "Demo: swap pendiente de integrar (ver app_python/). " +
            "Modelos en runtime: ${FaceSwapEngine.modelsDir(this).absolutePath}"
        Toast.makeText(this, "Placeholder: conecta el backend para el swap real", Toast.LENGTH_LONG).show()
    }
}
