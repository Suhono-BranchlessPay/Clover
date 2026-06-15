package com.branchlesspay.auditshield.clover

import com.google.gson.Gson
import com.google.gson.JsonParser
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

data class BpAnchorResult(
    val ok: Boolean,
    val httpStatus: Int,
    val anchorId: String?,
    val verifyUrl: String?,
    val status: String?,
    val contentHash: String?,
    val error: String?,
    val rawBody: String,
)

open class BpApiClient(
    private val licenseKey: String,
    private val apiUrl: String = DEFAULT_API_URL,
    private val client: OkHttpClient = defaultClient(),
) {
    companion object {
        const val DEFAULT_API_URL = "https://branchlesspay.com/api/v1/anchor"
        const val VERIFY_BASE = "https://branchlesspay.com/verify/"

        private val gson = Gson()
        private val jsonMedia = "application/json; charset=utf-8".toMediaType()

        fun defaultClient(): OkHttpClient =
            OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .build()

        fun legacyContentHash(payload: Map<String, Any>): String {
            val copy = payload.toMutableMap()
            copy.remove("content_hash")
            val canonical = gson.toJson(sortJson(copy))
            val digest = MessageDigest.getInstance("SHA-256")
            val bytes = digest.digest(canonical.toByteArray(Charsets.UTF_8))
            return bytes.joinToString("") { "%02x".format(it) }
        }

        @Suppress("UNCHECKED_CAST")
        private fun sortJson(value: Any?): Any? = when (value) {
            is Map<*, *> -> value.entries
                .sortedBy { it.key.toString() }
                .associate { it.key.toString() to sortJson(it.value) }
            is List<*> -> value.map { sortJson(it) }
            else -> value
        }
    }

    open fun postAnchor(payload: Map<String, Any>): BpAnchorResult {
        if (licenseKey.isBlank() || licenseKey.contains("YOUR_TOKEN")) {
            return BpAnchorResult(
                ok = false,
                httpStatus = 0,
                anchorId = null,
                verifyUrl = null,
                status = null,
                contentHash = null,
                error = "BP license key is not configured",
                rawBody = "",
            )
        }

        val bodyMap = payload.toMutableMap()
        bodyMap["content_hash"] = legacyContentHash(bodyMap)
        val json = gson.toJson(bodyMap)
        val request = Request.Builder()
            .url(apiUrl)
            .post(json.toRequestBody(jsonMedia))
            .header("Authorization", "Bearer $licenseKey")
            .header("Content-Type", "application/json")
            .build()

        client.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                return BpAnchorResult(
                    ok = false,
                    httpStatus = response.code,
                    anchorId = null,
                    verifyUrl = null,
                    status = null,
                    contentHash = null,
                    error = parseError(raw) ?: "HTTP ${response.code}",
                    rawBody = raw,
                )
            }

            val parsed = runCatching {
                JsonParser.parseString(raw).asJsonObject
            }.getOrNull()

            val anchorId = parsed?.get("anchor_id")?.asString
            return BpAnchorResult(
                ok = parsed?.get("ok")?.asBoolean ?: true,
                httpStatus = response.code,
                anchorId = anchorId,
                verifyUrl = anchorId?.let { VERIFY_BASE + it },
                status = parsed?.get("status")?.asString,
                contentHash = parsed?.get("content_hash")?.asString
                    ?: bodyMap["content_hash"] as? String,
                error = parsed?.get("error")?.asString,
                rawBody = raw,
            )
        }
    }

    private fun parseError(raw: String): String? {
        return runCatching {
            JsonParser.parseString(raw).asJsonObject.get("error")?.asString
        }.getOrNull() ?: raw.takeIf { it.isNotBlank() }?.take(200)
    }
}
