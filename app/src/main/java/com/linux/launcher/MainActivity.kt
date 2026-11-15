package com.linux.launcher

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 1. Ẩn thanh trạng thái (Status Bar) để giao diện HTML tràn viền
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(android.view.WindowInsets.Type.statusBars())
        } else {
            @Suppress("DEPRECATION")
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }

        // 2. Khởi tạo WebView
        webView = WebView(this)
        setContentView(webView)
        
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true // Bật lưu trữ LocalStorage cho HTML
        webView.webViewClient = WebViewClient()
        
        // Load file HTML
        webView.loadUrl("file:///android_asset/index.html")

        // 3. Xin quyền Cửa sổ nổi (Thanh tác vụ)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, 100)
        } else {
            startOverlayService()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100) {
            if (Settings.canDrawOverlays(this)) {
                startOverlayService()
            }
        }
    }

    private fun startOverlayService() {
        val serviceIntent = Intent(this, OverlayService::class.java)
        startService(serviceIntent)
    }
}
