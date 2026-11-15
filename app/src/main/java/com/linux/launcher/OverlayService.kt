package com.linux.launcher

import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.Toast

class OverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: FrameLayout
    private lateinit var webView: WebView

    override fun onBind(intent: Intent?): IBinder? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate() {
        super.onCreate()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }

        try {
            windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
            
            // 1. Tạo khung chứa trong suốt
            overlayView = FrameLayout(this).apply {
                setBackgroundColor(Color.TRANSPARENT)
            }

            // 2. Khởi tạo WebView trong suốt
            webView = WebView(this).apply {
                setBackgroundColor(Color.TRANSPARENT)
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = true
                
                // Cài đặt cầu nối để HTML gọi lệnh mở App
                addJavascriptInterface(WebAppInterface(), "AndroidApp")
                
                webViewClient = WebViewClient()
                loadUrl("file:///android_asset/index.html")
            }
            
            overlayView.addView(webView)

            // 3. Cấu hình cửa sổ nổi
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    WindowManager.LayoutParams.TYPE_PHONE
                },
                // FLAG_NOT_FOCUSABLE giúp bạn vẫn bấm được các app bên dưới thanh tác vụ
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            )
            params.gravity = Gravity.TOP

            windowManager.addView(overlayView, params)
            
        } catch (e: Exception) {
            e.printStackTrace()
            stopSelf()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            if (::overlayView.isInitialized) {
                windowManager.removeView(overlayView)
            }
        } catch (e: Exception) {}
    }

    // Lớp cầu nối cho phép JavaScript từ HTML gọi sang Android
    inner class WebAppInterface {
        @JavascriptInterface
        fun openApp(packageName: String) {
            try {
                val intent = packageManager.getLaunchIntentForPackage(packageName)
                if (intent != null) {
                    // Thử mở ở chế độ Cửa sổ tự do (Freeform)
                    val options = ActivityOptions.makeBasic()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        options.setLaunchWindowingMode(5) // 5 = WINDOWING_MODE_FREEFORM
                    }
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent, options.toBundle())
                } else {
                    Toast.makeText(this@OverlayService, "Không tìm thấy App: $packageName", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                // Nếu máy không hỗ trợ cửa sổ tự do, mở bình thường
                try {
                    val intent = packageManager.getLaunchIntentForPackage(packageName)
                    if (intent != null) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                    }
                } catch (ex: Exception) {
                    Toast.makeText(this@OverlayService, "Lỗi mở App", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
