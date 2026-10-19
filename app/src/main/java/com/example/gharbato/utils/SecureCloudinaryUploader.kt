package com.example.gharbato.utils

import android.content.Context
import android.net.Uri
import com.google.android.gms.tasks.Tasks
import com.google.firebase.functions.FirebaseFunctions
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import java.util.concurrent.TimeUnit

/** Performs signed uploads without ever receiving Cloudinary's API secret on the device. */
object SecureCloudinaryUploader {
    private const val BOUNDARY_PREFIX = "GharBatoUpload"

    fun uploadImage(context: Context, imageUri: Uri, assetKind: String): String {
        val function = FirebaseFunctions.getInstance(FirebaseFunctionsConfig.REGION)
            .getHttpsCallable("createCloudinaryUploadSignature")
        val response = Tasks.await(
            function.call(mapOf("kind" to assetKind)),
            30,
            TimeUnit.SECONDS
        )
        val data = response.data as? Map<*, *>
            ?: throw IOException("Upload authorization response was invalid")
        val cloudName = data["cloudName"] as? String
            ?: throw IOException("Upload service is not configured")
        val apiKey = data["apiKey"] as? String
            ?: throw IOException("Upload service is not configured")
        val signature = data["signature"] as? String
            ?: throw IOException("Upload authorization response was invalid")
        val params = data["params"] as? Map<*, *>
            ?: throw IOException("Upload authorization response was invalid")

        val fields = params.entries.associate { (key, value) ->
            key.toString() to value.toString()
        } + mapOf("api_key" to apiKey, "signature" to signature)

        return uploadMultipart(context, imageUri, cloudName, fields)
    }

    private fun uploadMultipart(
        context: Context,
        imageUri: Uri,
        cloudName: String,
        fields: Map<String, String>
    ): String {
        val boundary = "$BOUNDARY_PREFIX-${UUID.randomUUID()}"
        val connection = (URL("https://api.cloudinary.com/v1_1/$cloudName/image/upload")
            .openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 20_000
            readTimeout = 90_000
            setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            setChunkedStreamingMode(64 * 1024)
        }

        try {
            BufferedOutputStream(connection.outputStream).use { output ->
                fields.forEach { (name, value) ->
                    writeUtf8(output, "--$boundary\r\n")
                    writeUtf8(output, "Content-Disposition: form-data; name=\"$name\"\r\n\r\n")
                    writeUtf8(output, "$value\r\n")
                }

                val fileName = safeFileName(imageUri.lastPathSegment)
                val contentType = context.contentResolver.getType(imageUri) ?: "image/jpeg"
                writeUtf8(output, "--$boundary\r\n")
                writeUtf8(
                    output,
                    "Content-Disposition: form-data; name=\"file\"; filename=\"$fileName\"\r\n"
                )
                writeUtf8(output, "Content-Type: $contentType\r\n\r\n")

                val input = context.contentResolver.openInputStream(imageUri)
                    ?: throw IOException("Could not open the selected image")
                BufferedInputStream(input).use { source ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val count = source.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                    }
                }
                writeUtf8(output, "\r\n--$boundary--\r\n")
            }

            val status = connection.responseCode
            val responseText = (if (status in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader(Charsets.UTF_8)
                ?.use { it.readText() }
                .orEmpty()
            if (status !in 200..299) {
                throw IOException("Image upload failed (HTTP $status)")
            }

            val secureUrl = JSONObject(responseText).optString("secure_url")
            if (secureUrl.isBlank() || !secureUrl.startsWith("https://")) {
                throw IOException("Image upload returned an invalid URL")
            }
            return secureUrl
        } finally {
            connection.disconnect()
        }
    }

    private fun safeFileName(value: String?): String =
        value?.substringAfterLast('/')?.replace(Regex("[^A-Za-z0-9._-]"), "_")
            ?.take(100)?.takeIf { it.isNotBlank() } ?: "upload.jpg"

    private fun writeUtf8(output: BufferedOutputStream, value: String) {
        output.write(value.toByteArray(Charsets.UTF_8))
    }
}
