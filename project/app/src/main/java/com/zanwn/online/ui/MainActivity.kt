package com.zanwn.online.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.app.AlertDialog
import android.text.InputType
import android.widget.EditText
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.integration.android.IntentResult
import com.zanwn.online.bridge.AndroidBridge
import com.zanwn.online.data.ZanwnRepository
import com.zanwn.online.sync.SyncManager
import org.json.JSONObject

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private lateinit var syncManager: SyncManager
    private lateinit var androidBridge: AndroidBridge
    private var lockInProgress = false
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        webView = WebView(this).apply { alpha = 0f }
        setContentView(webView)
        val repository = ZanwnRepository(this)
        syncManager = SyncManager(this, repository)
        androidBridge = AndroidBridge(this, repository)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.allowFileAccess = true
        webView.settings.allowContentAccess = true
        webView.settings.databaseEnabled = true
        webView.webViewClient = WebViewClient()
        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) WebSettingsCompat.setForceDark(webView.settings, WebSettingsCompat.FORCE_DARK_OFF)
        webView.addJavascriptInterface(androidBridge, "Android")
        webView.loadUrl("file:///android_asset/index.html")
        syncManager.startPeriodic()
    }
    fun onWebAppReady(loggedIn: Boolean) { if (loggedIn) authenticateAppLock() else unlockApp() }
    fun authenticateAppLock() {
        if (lockInProgress) return
        lockInProgress = true
        runOnUiThread {
            val prompt = BiometricPrompt(this, ContextCompat.getMainExecutor(this), object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) { lockInProgress = false; unlockApp() }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) { lockInProgress = false; showPasscodeFallback() }
            })
            val info = BiometricPrompt.PromptInfo.Builder()
                .setTitle("فتح زين أونلاين")
                .setSubtitle("استخدم البصمة للمتابعة")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                .build()
            prompt.authenticate(info)
        }
    }
    private fun showPasscodeFallback() {
        if (!androidBridge.hasPin()) { authenticateDeviceCredential(); return }
        val input = EditText(this).apply { inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD; hint = "رمز التطبيق" }
        AlertDialog.Builder(this).setTitle("إدخال رمز التطبيق").setMessage("تعذر التحقق بالبصمة. أدخل الرمز الذي أنشأته بعد التسجيل.").setView(input)
            .setPositiveButton("دخول") { _, _ ->
                if (androidBridge.verifyPin(input.text.toString())) unlockApp()
                else { showPasscodeFallback() }
            }.setNegativeButton("إلغاء") { _, _ -> showPasscodeFallback() }.setOnCancelListener { showPasscodeFallback() }.show()
    }
    private fun authenticateDeviceCredential() {
        lockInProgress = true
        val prompt = BiometricPrompt(this, ContextCompat.getMainExecutor(this), object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) { lockInProgress = false; unlockApp() }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) { lockInProgress = false; showPasscodeFallback() }
        })
        val info = BiometricPrompt.PromptInfo.Builder().setTitle("فتح زين أونلاين").setSubtitle("استخدم رمز قفل الهاتف").setAllowedAuthenticators(BiometricManager.Authenticators.DEVICE_CREDENTIAL).build()
        prompt.authenticate(info)
    }
    fun unlockApp() { runOnUiThread { webView.alpha = 1f; webView.visibility = View.VISIBLE } }
    fun deliverBarcode(contents: String, mode: String) {
        val code = JSONObject.quote(contents); val scanMode = JSONObject.quote(mode)
        webView.post { webView.evaluateJavascript("window.onNativeBarcodeScanned && window.onNativeBarcodeScanned($code, $scanMode);", null) }
    }
    fun deliverCloudLinked(email: String) { webView.post { webView.evaluateJavascript("window.onNativeCloudAccountLinked && window.onNativeCloudAccountLinked(${JSONObject.quote(email)});", null) } }
    fun deliverCloudError(message: String) { webView.post { webView.evaluateJavascript("window.onNativeCloudAccountError && window.onNativeCloudAccountError(${JSONObject.quote(message)});", null) } }
    fun deliverCloudUnlinked() { webView.post { webView.evaluateJavascript("window.onNativeCloudAccountUnlinked && window.onNativeCloudAccountUnlinked();", null) } }
    @Deprecated("Android activity callback")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == AndroidBridge.GOOGLE_SIGN_IN_REQUEST) { androidBridge.handleGoogleSignInResult(data); return }
        val result: IntentResult? = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents != null) deliverBarcode(result.contents, androidBridge.currentScanMode())
            else webView.post { webView.evaluateJavascript("window.onNativeBarcodeCancelled && window.onNativeBarcodeCancelled();", null) }
        } else super.onActivityResult(requestCode, resultCode, data)
    }
    override fun onBackPressed() {
        webView.evaluateJavascript("window.nativeHandleBack ? window.nativeHandleBack() : false;") { handled ->
            if (handled != "true") {
                if (webView.canGoBack()) webView.goBack() else super@MainActivity.onBackPressed()
            }
        }
    }
}
