package com.branchlesspay.auditshield.clover

import android.content.Context
import android.content.Intent
import android.os.Build

object ServiceStarter {
    fun startAuditService(context: Context) {
        val intent = Intent(context, BpAuditService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }
}
