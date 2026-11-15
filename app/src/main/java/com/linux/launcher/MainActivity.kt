package com.linux.launcher

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.view.WindowManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            // Ẩn thanh trạng thái
            window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)

            webView = WebView(this)
            setContentView(webView)
            
            webView.settings.javaScriptEnabled = true
            webView.settings.domStorageEnabled = true
            webView.settings.allowFileAccess = true
            
            webView.webViewClient = WebViewClient()
            webView.loadUrl("file:///android_asset/index.html")
            
            Toast.makeText(this, "Khởi động thành công!", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            // Hiển thị lỗi đỏ lên màn hình thay vì văng app
            webView.loadData("<html><body style='background:red;color:white;'><h2>Lỗi: ${e.message}</h2></body></html>", "text/html", "UTF-8")
        }
    }
}
