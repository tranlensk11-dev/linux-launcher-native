package com.linux.launcher

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 1. Khởi tạo WebView
        webView = WebView(this)
        setContentView(webView)
        
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        
        // Bắt lỗi nếu không tìm thấy file HTML
        webView.webViewClient = object : WebViewClient() {
            override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                // Nếu lỗi, hiển thị chữ trên màn hình thay vì văng app
                view?.loadData("<html><body style='background:black;color:white;'><h1>Lỗi tải HTML: $description</h1></body></html>", "text/html", "UTF-8")
            }
        }
        
        // Load file HTML
        webView.loadUrl("file:///android_asset/index.html")

        // 2. Xin quyền Cửa sổ nổi (Bọc trong try-catch để không bị crash)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivityForResult(intent, 100)
            } else {
                startOverlayService()
            }
        } catch (e: Exception) {
            // Bỏ qua lỗi quyền, không làm văng app
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100) {
            try {
                if (Settings.canDrawOverlays(this)) {
                    startOverlayService()
                }
            } catch (e: Exception) {}
        }
    }

    private fun startOverlayService() {
        try {
            val serviceIntent = Intent(this, OverlayService::class.java)
            startService(serviceIntent)
        } catch (e: Exception) {
            // Bỏ qua lỗi khởi động Service nổi
        }
    }
}
