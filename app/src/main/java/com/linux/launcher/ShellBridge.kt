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
 * Cầu nối giữa index.html và hệ thống Android.
 *
 * - Terminal thật bằng /system/bin/sh hoặc su.
 * - Đọc, ghi, xóa, đổi tên file.
 * - Quản lý thư mục ứng dụng.
 */
class ShellBridge(
    private val webView: WebView,
    private val filesDir: File
) {

    private val procs = ConcurrentHashMap<String, Process>()

    // ---------- DIRECTORY ----------

    @JavascriptInterface
    fun homeDir(): String {
        return filesDir.absolutePath
    }

    @JavascriptInterface
    fun storageDir(): String {
        return Environment.getExternalStorageDirectory().absolutePath
    }

    // ---------- TERMINAL ----------

    @JavascriptInterface
    fun termStart(id: String, root: Boolean): String {
        termKill(id)

        return try {
            val command = if (root) {
                listOf("su")
            } else {
                listOf("/system/bin/sh")
            }

            val pb = ProcessBuilder(command)
            pb.directory(filesDir)
            pb.redirectErrorStream(true)
            pb.environment()["HOME"] = filesDir.absolutePath
            pb.environment()["TERM"] = "dumb"

            val process = pb.start()
            procs[id] = process

            Thread {
                try {
                    val buffer = ByteArray(4096)
                    val input = process.inputStream

                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break

                        val encoded = Base64.encodeToString(
                            buffer,
                            0,
                            count,
                            Base64.NO_WRAP
                        )
                        val safeId = JSONObject.quote(id)

                        webView.post {
                            webView.evaluateJavascript(
                                "window.__term($safeId,'$encoded')",
                                null
                            )
                        }
                    }
                } catch (_: Exception) {
                    // Tiến trình có thể bị đóng trong lúc đang đọc output.
                }

                val exitCode = try {
                    process.waitFor()
                } catch (_: Exception) {
                    -1
                }

                if (procs.remove(id, process)) {
                    val safeId = JSONObject.quote(id)

                    webView.post {
                        webView.evaluateJavascript(
                            "window.__termEnd($safeId,$exitCode)",
                            null
                        )
                    }
                }
            }.apply {
                name = "ShellBridge-$id"
                isDaemon = true
                start()
            }

            "ok"
        } catch (e: Exception) {
            "Lỗi: ${e.message ?: "Không thể khởi động terminal"}"
        }
    }

    @JavascriptInterface
    fun termWrite(id: String, text: String) {
        try {
            procs[id]?.outputStream?.let { output ->
                output.write(text.toByteArray(Charsets.UTF_8))
                output.flush()
            }
        } catch (_: Exception) {
            // Bỏ qua nếu terminal đã đóng.
        }
    }

    @JavascriptInterface
    fun termKill(id: String) {
        try {
            procs.remove(id)?.destroy()
        } catch (_: Exception) {
            // Bỏ qua nếu tiến trình đã kết thúc.
        }
    }

    // ---------- FILE MANAGER ----------

    @JavascriptInterface
    fun fsList(path: String): String {
        return try {
            val directory = File(path)
            val files = directory.listFiles() ?: return ""

            val sorted = files.sortedWith(
                compareBy<File>(
                    { !it.isDirectory },
                    { it.name.lowercase() }
                )
            )

            val output = JSONArray()

            sorted.forEach { file ->
                output.put(
                    JSONObject()
                        .put("n", file.name)
                        .put("d", file.isDirectory)
                        .put("s", if (file.isDirectory) 0 else file.length())
                )
            }

            output.toString()
        } catch (_: Exception) {
            ""
        }
    }

    @JavascriptInterface
    fun fsRead(path: String): String {
        return try {
            val file = File(path)

            if (!file.exists() || !file.isFile) {
                return ""
            }

            if (file.length() > 2_000_000) {
                return "(Tệp quá lớn để mở trong editor)"
            }

            file.readText(Charsets.UTF_8)
        } catch (_: Exception) {
            ""
        }
    }

    @JavascriptInterface
    fun fsWrite(path: String, text: String): Boolean {
        return try {
            File(path).writeText(text, Charsets.UTF_8)
            true
        } catch (_: Exception) {
            false
        }
    }

    @JavascriptInterface
    fun fsMkdir(path: String): Boolean {
        return try {
            File(path).mkdirs()
        } catch (_: Exception) {
            false
        }
    }

    @JavascriptInterface
    fun fsDelete(path: String): Boolean {
        return try {
            File(path).deleteRecursively()
        } catch (_: Exception) {
            false
        }
    }

    @JavascriptInterface
    fun fsRename(from: String, to: String): Boolean {
        return try {
            File(from).renameTo(File(to))
        } catch (_: Exception) {
            false
        }
    }
}
