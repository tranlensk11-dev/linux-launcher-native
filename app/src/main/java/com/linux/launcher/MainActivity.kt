 package com.linux.launcher

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast

class MainActivity : Activity() {

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
            
            webView.webViewClient = WebViewClient()
            
            // Load file HTML
            webView.loadUrl("file:///android_asset/index.html")
            
            Toast.makeText(this, "App đã khởi động!", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            // HIỆN LỖI LÊN MÀN HÌNH THAY VÌ VĂNG
            webView.loadData("<html><body style='background:red;color:white;'><h2>Lỗi: ${e.message}</h2></body></html>", "text/html", "UTF-8")
        }
    }
}
