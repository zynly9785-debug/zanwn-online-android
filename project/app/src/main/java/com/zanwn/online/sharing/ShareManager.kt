package com.zanwn.online.sharing

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

object ShareManager {
    private val whatsappPackages = listOf("com.whatsapp", "com.whatsapp.w4b")

    fun shareWhatsApp(context: Context, text: String) {
        val base = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text); addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        if (!startForInstalledWhatsApp(context, base)) shareChooser(context, text)
    }

    fun shareWhatsAppToNumber(context: Context, phone: String, text: String) {
        val cleanPhone = phone.replace("[^0-9]".toRegex(), "")
        val uri = Uri.parse("whatsapp://send?phone=$cleanPhone&text=" + Uri.encode(text))
        val direct = Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (startForInstalledWhatsApp(context, direct)) return
        // fallback لبعض إصدارات WhatsApp التي لا تسجل deep link المحلي
        val webUri = Uri.parse("https://wa.me/$cleanPhone?text=" + Uri.encode(text))
        val webIntent = Intent(Intent.ACTION_VIEW, webUri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (startForInstalledWhatsApp(context, webIntent)) return
        shareWhatsApp(context, text)
    }

    private fun startForInstalledWhatsApp(context: Context, base: Intent): Boolean {
        for (pkg in whatsappPackages) {
            try {
                val intent = Intent(base).setPackage(pkg)
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                    return true
                }
            } catch (_: ActivityNotFoundException) { }
        }
        return false
    }

    fun shareSms(context: Context, text: String) { context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:")).apply { putExtra("sms_body", text); addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }) }
    fun shareChooser(context: Context, text: String) { context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }, "مشاركة الفاتورة").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}
