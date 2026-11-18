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
    private val FILE_CHOOSER_RESULT_CODE = 10000

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)

            webView = WebView(this)
            setContentView(webView)
            
            webView.settings.javaScriptEnabled = true
            webView.settings.domStorageEnabled = true
            
            // QUAN TRỌNG: Cấp quyền đọc file local (để hiện ảnh logo và video)
            webView.settings.allowFileAccess = true
            webView.settings.allowContentAccess = true
            webView.settings.allowFileAccessFromFileURLs = true
            webView.settings.allowUniversalAccessFromFileURLs = true
            webView.settings.mediaPlaybackRequiresUserGesture = false // Cho phép video tự phát
            
            // Cấu hình chọn ảnh/video
            webView.webChromeClient = object : WebChromeClient() {
                override fun onShowFileChooser(
                    webView: WebView?,
                    filePathCallback: ValueCallback<Array<Uri>>?,
                    fileChooserParams: FileChooserParams?
                ): Boolean {
                    if (this@MainActivity.filePathCallback != null) {
                        this@MainActivity.filePathCallback!!.onReceiveValue(null)
                    }
                    this@MainActivity.filePathCallback = filePathCallback
                    val intent = fileChooserParams?.createIntent()
                    try {
                        startActivityForResult(intent, FILE_CHOOSER_RESULT_CODE)
                    } catch (e: Exception) {
                        this@MainActivity.filePathCallback = null
                        return false
                    }
                    return true
                }
            }

            // Cấu hình mở cửa sổ tự do
            webView.addJavascriptInterface(object {
                @JavascriptInterface
                fun openApp(packageName: String) {
                    runOnUiThread {
                        try {
                            val intent = packageManager.getLaunchIntentForPackage(packageName)
                            if (intent != null) {
                                val options = ActivityOptions.makeBasic()
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                    try {
                                        val method = options.javaClass.getMethod("setLaunchWindowingMode", Int::class.javaPrimitiveType)
                                        method.invoke(options, 5)
                                    } catch (e: Exception) { e.printStackTrace() }
                                }
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                startActivity(intent, options.toBundle())
                            }
                        } catch (e: Exception) {
                            try {
                                val intent = packageManager.getLaunchIntentForPackage(packageName)
                                if (intent != null) {
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    startActivity(intent)
                                }
                            } catch (ex: Exception) {}
                        }
                    }
                }
            }, "AndroidApp")

            // Terminal / File Manager / Editor thật (shell Android)
            webView.addJavascriptInterface(ShellBridge(webView, filesDir), "Shell")

            webView.webViewClient = WebViewClient()
            webView.loadUrl("file:///android_asset/index.html")

        } catch (e: Exception) {
            webView.loadData("<html><body style='background:red;color:white;'><h2>Lỗi: ${e.message}</h2></body></html>", "text/html", "UTF-8")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == FILE_CHOOSER_RESULT_CODE) {
            if (filePathCallback == null) return
            var results: Array<Uri>? = null
            if (resultCode == Activity.RESULT_OK && data != null) {
                val dataString = data.dataString
                if (dataString != null) {
                    results = arrayOf(Uri.parse(dataString))
                }
            }
            filePathCallback!!.onReceiveValue(results)
            filePathCallback = null
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }
}
