package com.example.hacprototype

import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import java.io.BufferedReader
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

class HabibiaApiException(val statusCode: Int, message: String) : Exception(message)

object HabibiaApi {
    const val BASE_URL = "http://10.0.2.2:8000"

    fun get(path: String, token: String? = HabibiaSession.authToken): String =
        request("GET", path, body = null, token = token)

    fun post(path: String, json: String, token: String? = HabibiaSession.authToken): String =
        request("POST", path, body = json, token = token)

    fun put(path: String, json: String, token: String? = HabibiaSession.authToken): String =
        request("PUT", path, body = json, token = token)

    fun delete(path: String, token: String? = HabibiaSession.authToken): String =
        request("DELETE", path, body = null, token = token)

    fun request(
        method: String,
        path: String,
        body: String?,
        token: String? = HabibiaSession.authToken
    ): String {
        val url = URL(BASE_URL + path)
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = 15000
            connection.readTimeout = 20000
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Content-Type", "application/json")
            if (!token.isNullOrBlank()) {
                connection.setRequestProperty("Authorization", "Bearer $token")
            }
            if (body != null) {
                connection.doOutput = true
                connection.outputStream.use { stream ->
                    stream.write(body.toByteArray(StandardCharsets.UTF_8))
                }
            }
            val code = connection.responseCode
            val stream: InputStream = if (code in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream ?: connection.inputStream
            }
            val text = stream.bufferedReader(StandardCharsets.UTF_8).use(BufferedReader::readText)
            if (code !in 200..299) {
                throw HabibiaApiException(code, text.ifBlank { "Request failed ($code)" })
            }
            return text
        } finally {
            connection.disconnect()
        }
    }
}

fun Fragment.apiInBackground(
    work: () -> String,
    onOk: (String) -> Unit,
    onError: (String) -> Unit = { showMessage(it) }
) {
    Thread {
        try {
            val result = work()
            activity?.runOnUiThread { onOk(result) } ?: Handler(Looper.getMainLooper()).post { onOk(result) }
        } catch (error: HabibiaApiException) {
            val message = when (error.statusCode) {
                401 -> "Please log in again"
                403 -> "You do not have permission to do that"
                else -> error.message ?: "Request failed"
            }
            activity?.runOnUiThread { onError(message) }
        } catch (error: Exception) {
            activity?.runOnUiThread { onError(error.message ?: "Could not reach the API. Is it running?") }
        }
    }.start()
}
