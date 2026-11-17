
package com.linux.launcher

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient

class MainActivity : Activity() {

    private lateinit var webView: WebView

    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    companion object {
        private const val FILE_CHOOSER_RESULT_CODE = 10000
    }

    @SuppressLint(
        "SetJavaScriptEnabled",
        "AllowFileAccess",
        "AllowFileAccessFromFileURLs",
        "AllowUniversalAccessFromFileURLs"
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            // ---------- FULL SCREEN ----------

            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )

            // ---------- WEBVIEW ----------

            webView = WebView(this)

            setContentView(webView)

            webView.settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true

                allowFileAccess = true
                allowContentAccess = true

                allowFileAccessFromFileURLs = true
                allowUniversalAccessFromFileURLs = true

                mediaPlaybackRequiresUserGesture = false

                javaScriptCanOpenWindowsAutomatically = true
                setSupportMultipleWindows(true)
            }

            // ---------- FILE CHOOSER ----------

            webView.webChromeClient = object : WebChromeClient() {

                override fun onShowFileChooser(
                    webView: WebView?,
                    callback: ValueCallback<Array<Uri>>?,
                    params: FileChooserParams?
                ): Boolean {

                    // Hủy callback cũ nếu còn tồn tại
                    filePathCallback?.onReceiveValue(null)
                    filePathCallback = callback

                    if (callback == null) {
                        return false
                    }

                    return try {
                        val intent = params?.createIntent()

                        if (intent == null) {
                            filePathCallback?.onReceiveValue(null)
                            filePathCallback = null
                            return false
                        }

                        startActivityForResult(
                            intent,
                            FILE_CHOOSER_RESULT_CODE
                        )

                        true

                    } catch (e: Exception) {
                        filePathCallback?.onReceiveValue(null)
                        filePathCallback = null
                        false
                    }
                }
            }

            // ---------- ANDROID APP BRIDGE ----------

            webView.addJavascriptInterface(
                object {

                    @JavascriptInterface
                    fun openApp(packageName: String) {

                        runOnUiThread {

                            try {
                                val intent =
                                    packageManager.getLaunchIntentForPackage(
                                        packageName
                                    )

                                if (intent != null) {

                                    intent.addFlags(
                                        Intent.FLAG_ACTIVITY_NEW_TASK
                                    )

                                    try {
                                        val options =
                                            ActivityOptions.makeBasic()

                                        if (
                                            Build.VERSION.SDK_INT >=
                                            Build.VERSION_CODES.N
                                        ) {
                                            try {
                                                val method =
                                                    options.javaClass.getMethod(
                                                        "setLaunchWindowingMode",
                                                        Int::class.javaPrimitiveType
                                                    )

                                                // 5 = Freeform window mode
                                                method.invoke(options, 5)

                                            } catch (_: Exception) {
                                                // Thiết bị không hỗ trợ
                                                // chế độ cửa sổ tự do
                                            }
                                        }

                                        startActivity(
                                            intent,
                                            options.toBundle()
                                        )

                                    } catch (_: Exception) {

                                        startActivity(intent)
                                    }
                                }

                            } catch (_: Exception) {
                                // Không thể mở ứng dụng
                            }
                        }
                    }

                },
                "AndroidApp"
            )

            // ---------- SHELL BRIDGE ----------

            // QUAN TRỌNG:
            // ShellBridge phiên bản mới nhận 2 tham số:
            // WebView và filesDir.

            webView.addJavascriptInterface(
                ShellBridge(webView, filesDir),
                "Shell"
            )

            // ---------- WEBVIEW CLIENT ----------

            webView.webViewClient = object : WebViewClient() {

                override fun onPageFinished(
                    view: WebView?,
                    url: String?
                ) {
                    super.onPageFinished(view, url)
                }
            }

            // ---------- LOAD LINUX DESKTOP ----------

            webView.loadUrl(
                "file:///android_asset/index.html"
            )

        } catch (e: Exception) {

            val errorMessage = e.message ?: "Unknown error"

            webViewFallback(errorMessage)
        }
    }

    // ---------- WEBVIEW ERROR ----------

    private fun webViewFallback(message: String) {

        val safeMessage = message
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")

        val html = """
            <html>
            <head>
                <meta name="viewport"
                    content="width=device-width, initial-scale=1">
            </head>
            <body style="
                background:#180000;
                color:white;
                font-family:Arial;
                padding:24px;
            ">
                <h2>Linux Launcher Error</h2>
                <p>$safeMessage</p>
            </body>
            </html>
        """.trimIndent()

        try {
            webView.loadDataWithBaseURL(
                null,
                html,
                "text/html",
                "UTF-8",
                null
            )
        } catch (_: Exception) {
        }
    }

    // ---------- FILE PICKER RESULT ----------

    @Deprecated("Deprecated in Android API")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(
            requestCode,
            resultCode,
            data
        )

        if (requestCode == FILE_CHOOSER_RESULT_CODE) {

            val callback = filePathCallback ?: return

            var results: Array<Uri>? = null

            if (resultCode == Activity.RESULT_OK && data != null) {

                val clipData = data.clipData

                if (clipData != null) {

                    results = Array(clipData.itemCount) { index ->
                        clipData.getItemAt(index).uri
                    }

                } else {

                    val uri = data.data

                    if (uri != null) {
                        results = arrayOf(uri)
                    }
                }
            }

            callback.onReceiveValue(results)

            filePathCallback = null
        }
    }

    // ---------- ACTIVITY DESTROY ----------

    override fun onDestroy() {

        filePathCallback?.onReceiveValue(null)
        filePathCallback = null

        if (::webView.isInitialized) {
            webView.stopLoading()
            webView.removeJavascriptInterface("Shell")
            webView.removeJavascriptInterface("AndroidApp")
            webView.destroy()
        }

        super.onDestroy()
    }
}
