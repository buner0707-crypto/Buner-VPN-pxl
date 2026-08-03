package com.buner.vpn

import android.content.Context
import android.content.SharedPreferences
import java.util.UUID

class AccessKeyManager private constructor(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("vpn_prefs", Context.MODE_PRIVATE)

    fun getOrCreateKey(): String {
        val existing = prefs.getString("access_key", null)
        if (existing != null) return existing

        val newKey = UUID.randomUUID().toString().replace("-", "").substring(0, 16)
        prefs.edit().putString("access_key", newKey).apply()
        return newKey
    }

    companion object {
        @Volatile
        private var instance: AccessKeyManager? = null

        fun getInstance(context: Context): AccessKeyManager {
            return instance ?: synchronized(this) {
                instance ?: AccessKeyManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
