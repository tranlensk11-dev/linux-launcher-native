package com.linux.launcher

import android.util.Base64
import android.webkit.JavascriptInterface
import android.webkit.WebView
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class ShellBridge(private val wv: WebView, private val home: File) {
    private val procs = ConcurrentHashMap<String, Process>()

    private fun js(id: String, data: ByteArray?, done: Boolean, code: Int) {
        val b = if (data == null) "" else Base64.encodeToString(data, Base64.NO_WRAP)
        wv.post { wv.evaluateJavascript("window.__sh('$id','$b',$done,$code)", null) }
    }

    @JavascriptInterface
    fun home(): String = home.absolutePath

    @JavascriptInterface
    fun start(id: String, cmd: String, cwd: String) {
        Thread {
            try {
                val dir = File(cwd)
                if (!dir.isDirectory) {
                    js(id, "cd: $cwd: không tồn tại\n".toByteArray(), true, 1)
                    return@Thread
                }
                val p = ProcessBuilder("/system/bin/sh", "-c", cmd)
                    .directory(dir)
                    .redirectErrorStream(true)
                    .start()
                procs[id] = p
                p.outputStream.close()
                val buf = ByteArray(4096)
                p.inputStream.use { s ->
                    while (true) {
                        val n = s.read(buf)
                        if (n < 0) break
                        js(id, buf.copyOf(n), false, 0)
                    }
                }
                val code = p.waitFor()
                procs.remove(id)
                js(id, null, true, code)
            } catch (e: Exception) {
                js(id, "lỗi: ${e.message}\n".toByteArray(), true, 1)
            }
        }.start()
    }

    @JavascriptInterface
    fun kill(id: String) {
        procs[id]?.destroy()
    }
}
