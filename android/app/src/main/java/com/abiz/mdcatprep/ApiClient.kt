package com.abiz.mdcatprep

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.Executors

/** Error surfaced from the API envelope: {"ok":false,"error":{code,message}}. */
class ApiException(val code: String, message: String, val httpStatus: Int = 0) : Exception(message)

/**
 * Minimal networking layer on HttpURLConnection + org.json (no Retrofit).
 * All calls run on a background executor; callbacks fire on the main thread.
 */
object ApiClient {
    private val executor = Executors.newCachedThreadPool()
    private val main = Handler(Looper.getMainLooper())

    fun get(
        route: String,
        params: Map<String, String> = emptyMap(),
        token: String? = null,
        onCache: ((JSONObject) -> Unit)? = null,
        onOk: (JSONObject) -> Unit,
        onErr: (ApiException) -> Unit
    ) {
        var url = ApiConfig.url(route)
        if (params.isNotEmpty()) {
            val qs = params.entries.joinToString("&") {
                URLEncoder.encode(it.key, "UTF-8") + "=" + URLEncoder.encode(it.value, "UTF-8")
            }
            url += "&" + qs
        }
        // Offline handling: every successful response is stored. When a
        // later request fails (no internet, server down), the stored
        // copy is served through onCache so screens show real content
        // instead of an endless loading state.
        val key = route + "|" + params.entries.sortedBy { it.key }
            .joinToString("&") { it.key + "=" + it.value } +
            "|u" + (token?.hashCode() ?: 0)
        request("GET", url, null, token,
            onOk = { out ->
                runCatching { Prefs.cachePut(key, out.toString()) }
                onOk(out)
            },
            onErr = { e ->
                val cached = if (onCache != null)
                    runCatching { Prefs.cacheGet(key) }.getOrNull() else null
                if (cached != null && onCache != null) {
                    try {
                        onCache(JSONObject(cached))
                    } catch (x: Exception) {
                        onErr(e)
                    }
                } else {
                    onErr(e)
                }
            })
    }

    fun post(
        route: String,
        body: JSONObject,
        token: String? = null,
        onOk: (JSONObject) -> Unit,
        onErr: (ApiException) -> Unit
    ) {
        request("POST", ApiConfig.url(route), body, token, onOk, onErr)
    }

    private fun request(
        method: String,
        urlStr: String,
        body: JSONObject?,
        token: String?,
        onOk: (JSONObject) -> Unit,
        onErr: (ApiException) -> Unit
    ) {
        executor.execute {
            try {
                val conn = URL(urlStr).openConnection() as HttpURLConnection
                conn.requestMethod = method
                conn.connectTimeout = 12000
                conn.readTimeout = 20000
                conn.setRequestProperty("Accept", "application/json")
                if (!token.isNullOrEmpty()) {
                    conn.setRequestProperty("Authorization", "Bearer $token")
                }
                if (body != null) {
                    conn.doOutput = true
                    conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    OutputStreamWriter(conn.outputStream, "UTF-8").use { it.write(body.toString()) }
                }
                val status = conn.responseCode
                val stream = if (status in 200..299) conn.inputStream else conn.errorStream
                val text = stream?.let {
                    BufferedReader(InputStreamReader(it, "UTF-8")).use { r -> r.readText() }
                } ?: ""
                val json = try {
                    JSONObject(text)
                } catch (e: Exception) {
                    throw ApiException("bad_response", "The server returned an unreadable response.", status)
                }
                if (json.optBoolean("ok", false)) {
                    // data may be a JSONObject or JSONArray; wrap arrays so callers
                    // always receive a JSONObject with an "items" key.
                    val data = json.opt("data")
                    val out = when (data) {
                        is JSONObject -> data
                        is org.json.JSONArray -> JSONObject().put("items", data)
                        else -> JSONObject()
                    }
                    main.post { onOk(out) }
                } else {
                    val err = json.optJSONObject("error")
                    val code = err?.optString("code", "error") ?: "error"
                    val msg = err?.optString("message", "Something went wrong.") ?: "Something went wrong."
                    throw ApiException(code, msg, status)
                }
            } catch (e: ApiException) {
                main.post { onErr(e) }
            } catch (e: Exception) {
                main.post {
                    onErr(ApiException("network", "Could not reach the server. Check your connection and try again."))
                }
            }
        }
    }

    /** Loads a bitmap from a URL (used for banner images; no image library). */
    fun loadBitmap(urlStr: String, onOk: (Bitmap) -> Unit, onErr: () -> Unit) {
        if (urlStr.isEmpty()) {
            onErr(); return
        }
        executor.execute {
            try {
                val conn = URL(urlStr).openConnection() as HttpURLConnection
                conn.connectTimeout = 10000
                conn.readTimeout = 15000
                conn.inputStream.use { input ->
                    val bmp = BitmapFactory.decodeStream(input)
                    if (bmp != null) main.post { onOk(bmp) } else main.post { onErr() }
                }
            } catch (e: Exception) {
                main.post { onErr() }
            }
        }
    }
}
