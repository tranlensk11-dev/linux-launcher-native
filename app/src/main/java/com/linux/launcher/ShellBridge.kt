package com.linux.launcher

import android.os.Environment
import android.util.Base64
import android.webkit.JavascriptInterface
import android.webkit.WebView
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Cầu nối THẬT giữa index.html và hệ thống Android:
 *  - Shell thật (/system/bin/sh, hoặc su nếu máy đã root)
 *  - Đọc/ghi/xóa/đổi tên file thật
 */
class ShellBridge(private val webView: WebView) {
    private val procs = ConcurrentHashMap<String, Process>()
    private val ctx = webView.context

    @JavascriptInterface
    fun homeDir(): String = ctx.filesDir.absolutePath

    @JavascriptInterface
    fun storageDir(): String = Environment.getExternalStorageDirectory().absolutePath

    // ---------- TERMINAL ----------
    @JavascriptInterface
    fun termStart(id: String, root: Boolean): String {
        termKill(id)
        return try {
            val pb = ProcessBuilder(if (root) listOf("su") else listOf("/system/bin/sh"))
            pb.directory(ctx.filesDir)
            pb.redirectErrorStream(true)
            pb.environment()["HOME"] = ctx.filesDir.absolutePath
            pb.environment()["TERM"] = "dumb"
            val p = pb.start()
            procs[id] = p
            Thread {
                try {
                    val buf = ByteArray(4096)
                    val ins = p.inputStream
                    while (true) {
                        val n = ins.read(buf)
                        if (n < 0) break
                        val b64 = Base64.encodeToString(buf, 0, n, Base64.NO_WRAP)
                        webView.post { webView.evaluateJavascript("window.__term('$id','$b64')", null) }
                    }
                } catch (_: Exception) {
                }
                val code = try { p.waitFor() } catch (_: Exception) { -1 }
                if (procs.remove(id, p)) {
                    webView.post { webView.evaluateJavascript("window.__termEnd('$id',$code)", null) }
                }
            }.start()
            "ok"
        } catch (e: Exception) {
            "Lỗi: ${e.message}"
        }
    }

    @JavascriptInterface
    fun termWrite(id: String, text: String) {
        try {
            procs[id]?.outputStream?.let { it.write(text.toByteArray()); it.flush() }
        } catch (_: Exception) {
        }
    }

    @JavascriptInterface
    fun termKill(id: String) {
        procs.remove(id)?.destroy()
    }

    // ---------- FILES ----------
    @JavascriptInterface
    fun fsList(path: String): String {
        val arr = File(path).listFiles() ?: return ""
        val sorted = arr.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
        val out = JSONArray()
        sorted.forEach {
            out.put(JSONObject().put("n", it.name).put("d", it.isDirectory).put("s", if (it.isDirectory) 0 else it.length()))
        }
        return out.toString()
    }

    @JavascriptInterface
    fun fsRead(path: String): String = try {
        val f = File(path)
        if (f.length() > 2_000_000) "(Tệp quá lớn để mở trong editor)" else f.readText()
    } catch (e: Exception) {
        ""
    }

    @JavascriptInterface
    fun fsWrite(path: String, text: String): Boolean = try {
        File(path).writeText(text); true
    } catch (e: Exception) {
        false
    }

    @JavascriptInterface
    fun fsMkdir(path: String): Boolean = try { File(path).mkdirs() } catch (e: Exception) { false }

    @JavascriptInterface
    fun fsDelete(path: String): Boolean = try { File(path).deleteRecursively() } catch (e: Exception) { false }

    @JavascriptInterface
    fun fsRename(from: String, to: String): Boolean = try { File(from).renameTo(File(to)) } catch (e: Exception) { false }
}
