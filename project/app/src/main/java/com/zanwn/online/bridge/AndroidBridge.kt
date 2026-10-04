package com.zanwn.online.bridge

import android.content.Context
import android.content.Intent
import android.webkit.JavascriptInterface
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.zxing.integration.android.IntentIntegrator
import com.zanwn.online.data.ZanwnRepository
import com.zanwn.online.security.SecurityManager
import kotlinx.coroutines.runBlocking
import org.json.JSONObject

class AndroidBridge(private val context: Context, private val repository: ZanwnRepository) {
    companion object { const val GOOGLE_SIGN_IN_REQUEST = 9012 }
    private val prefs = context.getSharedPreferences("native_state", Context.MODE_PRIVATE)
    private val security by lazy { SecurityManager(context) }
    @Volatile private var scanMode = "sale"
    @JavascriptInterface fun isNativeApp(): Boolean = true
    @JavascriptInterface fun loadSnapshot(): String = prefs.getString("snapshot", "{}") ?: "{}"
    @JavascriptInterface fun saveSnapshot(snapshot: String): Boolean = try { prefs.edit().putString("snapshot", snapshot).apply(); runBlocking { repository.queue("app_snapshot", "update", "current", snapshot) }; true } catch (_: Exception) { false }
    @JavascriptInterface fun getCurrentUserId(): String = prefs.getString("user_id", "local") ?: "local"
    @JavascriptInterface fun setCurrentUserId(id: String) { prefs.edit().putString("user_id", id).apply() }
    @JavascriptInterface fun signInWithGoogleIdToken(idToken: String) { runCatching { FirebaseAuth.getInstance().signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).addOnSuccessListener { result -> setCurrentUserId(result.user?.uid ?: "local") } } }
    @JavascriptInterface fun appReady(loggedIn: Boolean) { (context as? com.zanwn.online.ui.MainActivity)?.onWebAppReady(loggedIn) }
    @JavascriptInterface fun authenticateAppLock() { (context as? com.zanwn.online.ui.MainActivity)?.authenticateAppLock() }

    @JavascriptInterface fun cloudAccountEmail(): String = prefs.getString("cloud_email", "") ?: ""
    @JavascriptInterface fun linkGoogleAccount() {
        val activity = context as? FragmentActivity ?: return
        val webClientId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName).takeIf { it != 0 }?.let { context.getString(it) }.orEmpty()
        if (webClientId.isBlank() || webClientId.contains("REPLACE")) {
            (activity as? com.zanwn.online.ui.MainActivity)?.deliverCloudError("يجب استبدال google-services.json بملف Firebase الحقيقي أولاً")
            return
        }
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().requestIdToken(webClientId).build()
        activity.startActivityForResult(GoogleSignIn.getClient(activity, options).signInIntent, GOOGLE_SIGN_IN_REQUEST)
    }
    fun handleGoogleSignInResult(data: Intent?) {
        val activity = context as? FragmentActivity ?: return
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        if (!task.isSuccessful) { (activity as? com.zanwn.online.ui.MainActivity)?.deliverCloudError("تم إلغاء ربط حساب Google أو تعذر اختياره"); return }
        val account = task.result
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        FirebaseAuth.getInstance().signInWithCredential(credential).addOnCompleteListener { result ->
            if (result.isSuccessful) {
                val user = result.result?.user
                prefs.edit().putString("user_id", user?.uid ?: "").putString("cloud_email", account.email ?: "").apply()
                (activity as? com.zanwn.online.ui.MainActivity)?.deliverCloudLinked(account.email ?: "")
            } else (activity as? com.zanwn.online.ui.MainActivity)?.deliverCloudError(result.exception?.message ?: "تعذر ربط حساب Google")
        }
    }
    @JavascriptInterface fun unlinkCloudAccount() {
        FirebaseAuth.getInstance().signOut()
        prefs.edit().remove("user_id").remove("cloud_email").apply()
        (context as? com.zanwn.online.ui.MainActivity)?.deliverCloudUnlinked()
    }

    @JavascriptInterface fun hasPin(): Boolean = runCatching { security.hasPin() }.getOrDefault(false)
    @JavascriptInterface fun setPin(pin: String): Boolean = runCatching { if (pin.length < 4) false else { security.setPin(pin); true } }.getOrDefault(false)
    @JavascriptInterface fun verifyPin(pin: String): Boolean = runCatching { security.verifyPin(pin) }.getOrDefault(false)
    @JavascriptInterface fun biometricAvailable(): Boolean = runCatching { security.biometricAvailable() }.getOrDefault(false)
    @JavascriptInterface fun authenticateBiometric() {
        val activity = context as? FragmentActivity ?: return
        activity.runOnUiThread { runCatching {
            val prompt = BiometricPrompt(activity, activity.mainExecutor, object : BiometricPrompt.AuthenticationCallback() {})
            val info = BiometricPrompt.PromptInfo.Builder().setTitle("تسجيل الدخول إلى زين أونلاين").setSubtitle("استخدم البصمة للمتابعة").setNegativeButtonText("إلغاء").build()
            prompt.authenticate(info)
        } }
    }
    @JavascriptInterface fun scanBarcode(mode: String) {
        val activity = context as? FragmentActivity ?: return
        scanMode = if (mode == "inventory") "inventory" else "sale"
        activity.runOnUiThread { runCatching {
            IntentIntegrator(activity).apply {
                setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES)
                setPrompt(if (scanMode == "inventory") "امسح باركود المنتج لإضافته أو تعديله" else "امسح باركود المنتج لإضافته إلى سلة الفاتورة")
                setBeepEnabled(true); setOrientationLocked(false)
            }.initiateScan()
        } }
    }
    fun currentScanMode(): String = scanMode
    @JavascriptInterface fun shareWhatsApp(text: String) = com.zanwn.online.sharing.ShareManager.shareWhatsApp(context, text)
    @JavascriptInterface fun shareWhatsAppToNumber(phone: String, text: String) = com.zanwn.online.sharing.ShareManager.shareWhatsAppToNumber(context, phone, text)
    @JavascriptInterface fun shareSms(text: String) = com.zanwn.online.sharing.ShareManager.shareSms(context, text)
    @JavascriptInterface fun shareText(text: String) = com.zanwn.online.sharing.ShareManager.shareChooser(context, text)
    @JavascriptInterface fun syncNow() { com.zanwn.online.sync.SyncManager(context, repository).syncNow() }
    @JavascriptInterface fun nativeStatus(): String = JSONObject().put("offlineFirst", true).put("room", true).put("firebase", true).put("googleAuth", true).put("cloudEmail", cloudAccountEmail()).put("biometric", biometricAvailable()).put("barcodeScanner", true).toString()
}
