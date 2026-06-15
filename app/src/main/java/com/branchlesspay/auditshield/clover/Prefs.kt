package com.branchlesspay.auditshield.clover

import android.content.Context
import android.content.SharedPreferences

object Prefs {
    private const val NAME = "bp_audit_shield"
    private const val KEY_LICENSE = "bp_license_key"
    private const val KEY_API_URL = "bp_api_url"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun getLicenseKey(context: Context): String =
        prefs(context).getString(KEY_LICENSE, "")?.trim().orEmpty()

    fun getApiUrl(context: Context): String =
        prefs(context).getString(KEY_API_URL, BpApiClient.DEFAULT_API_URL)?.trim()
            ?: BpApiClient.DEFAULT_API_URL

    fun save(context: Context, licenseKey: String, apiUrl: String) {
        prefs(context).edit()
            .putString(KEY_LICENSE, licenseKey.trim())
            .putString(KEY_API_URL, apiUrl.trim().ifEmpty { BpApiClient.DEFAULT_API_URL })
            .apply()
    }

    fun isConfigured(context: Context): Boolean =
        getLicenseKey(context).isNotEmpty()
}
