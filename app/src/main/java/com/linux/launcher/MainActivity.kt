package com.linux.launcher

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            webView = WebView(this)
            setContentView(webView)
            
            webView.settings.javaScriptEnabled = true
            webView.settings.domStorageEnabled = true
            webView.settings.allowFileAccess = true
            webView.settings.allowContentAccess = true
            
            // Bắt lỗi nếu HTML không tải được (thay vì văng app)
            webView.webViewClient = object : WebViewClient() {
                override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                    view?.loadData("<html><body style='background:black;color:red;'><h2>Lỗi tải HTML: $description</h2></body></html>", "text/html", "UTF-8")
                }
            }
            
            webView.loadUrl("file:///android_asset/index.html")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                startActivityForResult(intent, 100)
            } else {
                startOverlayService()
            }
        } catch (e: Exception) {
            // Nếu có lỗi chí mạng, ghi log ra thay vì văng app
            e.printStackTrace()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
            startOverlayService()
        }
    }

    private fun startOverlayService() {
        try {
            val serviceIntent = Intent(this, OverlayService::class.java)
            startService(serviceIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
