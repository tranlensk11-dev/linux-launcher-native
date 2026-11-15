package com.linux.launcher

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 1. Xin quyền hiển thị trên ứng dụng khác
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Vui lòng cấp quyền hiển thị trên ứng dụng khác", Toast.LENGTH_LONG).show()
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, 100)
        } else {
            startOverlayAndFinish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                startOverlayAndFinish()
            } else {
                Toast.makeText(this, "Chưa cấp quyền, không thể chạy thanh tác vụ!", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun startOverlayAndFinish() {
        try {
            // Khởi động Service chứa thanh tác vụ
            val serviceIntent = Intent(this, OverlayService::class.java)
            startService(serviceIntent)
            
            // Đóng Activity này lại để quay về màn hình chính
            finish()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
