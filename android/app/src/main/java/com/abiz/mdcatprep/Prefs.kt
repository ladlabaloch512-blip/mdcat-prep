package com.abiz.mdcatprep

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import org.json.JSONObject

/** Small SharedPreferences wrapper: auth session + cached settings + design. */
object Prefs {
    private const val FILE = "mdcat_prefs"
    private lateinit var sp: SharedPreferences

    fun init(ctx: Context) {
        if (!::sp.isInitialized) sp = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)
    }

    // The auth token is stored encrypted with an Android Keystore key
    // (AES-GCM), never as plain text. Values written by older builds
    // (no "v1:" prefix) are still read, and are re-encrypted on the
    // next sign-in.
    var token: String?
        get() {
            val raw = sp.getString("token", null)?.takeIf { it.isNotEmpty() } ?: return null
            if (!raw.startsWith("v1:")) return raw
            return decryptToken(raw.substring(3))
        }
        set(v) {
            if (v.isNullOrEmpty()) {
                sp.edit().putString("token", "").apply()
                return
            }
            val enc = encryptToken(v)
            sp.edit().putString("token", if (enc != null) "v1:" + enc else v).apply()
        }

    private fun tokenKey(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore")
        ks.load(null)
        (ks.getKey("mdcatprep_token_key", null) as? SecretKey)?.let { return it }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        gen.init(
            KeyGenParameterSpec.Builder(
                "mdcatprep_token_key",
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
        )
        return gen.generateKey()
    }

    private fun encryptToken(plain: String): String? = try {
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.ENCRYPT_MODE, tokenKey())
        Base64.encodeToString(c.iv + c.doFinal(plain.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)
    } catch (e: Exception) { null }

    private fun decryptToken(b64: String): String? = try {
        val all = Base64.decode(b64, Base64.NO_WRAP)
        val iv = all.copyOfRange(0, 12)
        val data = all.copyOfRange(12, all.size)
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.DECRYPT_MODE, tokenKey(), GCMParameterSpec(128, iv))
        String(c.doFinal(data), Charsets.UTF_8)
    } catch (e: Exception) { null }

    var guest: Boolean
        get() = sp.getBoolean("guest", false)
        set(v) = sp.edit().putBoolean("guest", v).apply()

    var userJson: String?
        get() = sp.getString("user_json", null)
        set(v) = sp.edit().putString("user_json", v).apply()

    fun user(): JSONObject? = userJson?.let { runCatching { JSONObject(it) }.getOrNull() }

    var designId: String?
        get() = sp.getString("design_id", null)?.takeIf { it.isNotEmpty() }
        set(v) = sp.edit().putString("design_id", v ?: "").apply()

    /** Each design remembers its own colour palette index. */
    fun paletteIndex(designId: String): Int = sp.getInt("palette_" + designId, 0)

    fun setPaletteIndex(designId: String, idx: Int) {
        sp.edit().putInt("palette_" + designId, idx).apply()
    }

    var settingsJson: String?
        get() = sp.getString("settings_json", null)
        set(v) = sp.edit().putString("settings_json", v).apply()

    fun settings(): JSONObject? = settingsJson?.let { runCatching { JSONObject(it) }.getOrNull() }

    // Offline cache: last successful GET response per route, so the
    // app keeps showing real content when the server is unreachable
    // instead of sitting on a loading message.
    fun cachePut(key: String, value: String) {
        sp.edit().putString("cache_" + key, value).apply()
    }

    fun cacheGet(key: String): String? = sp.getString("cache_" + key, null)

    fun clearSession() {
        sp.edit().remove("token").remove("user_json").putBoolean("guest", false).apply()
    }
}
