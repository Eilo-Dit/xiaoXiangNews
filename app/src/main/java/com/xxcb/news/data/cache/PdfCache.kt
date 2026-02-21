package com.xxcb.news.data.cache

import android.content.Context
import java.io.File
import java.net.URL
import java.security.MessageDigest

object PdfCache {

    private const val PDF_CACHE_DIR = "pdf_cache"
    private const val MAX_CACHE_SIZE_MB = 200L

    private fun getCacheDir(context: Context): File {
        val dir = File(context.cacheDir, PDF_CACHE_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun urlToFileName(url: String): String {
        val md5 = MessageDigest.getInstance("MD5")
            .digest(url.toByteArray())
            .joinToString("") { "%02x".format(it) }
        return "$md5.pdf"
    }

    fun getCachedFile(context: Context, url: String): File? {
        val file = File(getCacheDir(context), urlToFileName(url))
        return if (file.exists() && file.length() > 0) file else null
    }

    fun downloadAndCache(context: Context, url: String): File {
        val cacheDir = getCacheDir(context)
        val file = File(cacheDir, urlToFileName(url))

        if (file.exists() && file.length() > 0) {
            return file
        }

        // Download
        val connection = URL(url).openConnection()
        connection.connectTimeout = 15000
        connection.readTimeout = 15000
        connection.getInputStream().use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        // Trim cache if too large
        trimCache(cacheDir)

        return file
    }

    private fun trimCache(cacheDir: File) {
        val maxBytes = MAX_CACHE_SIZE_MB * 1024 * 1024
        val files = cacheDir.listFiles()?.sortedBy { it.lastModified() } ?: return
        var totalSize = files.sumOf { it.length() }

        for (file in files) {
            if (totalSize <= maxBytes) break
            totalSize -= file.length()
            file.delete()
        }
    }
}
