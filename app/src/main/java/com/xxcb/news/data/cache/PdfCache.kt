package com.xxcb.news.data.cache

import android.content.Context
import android.graphics.Bitmap
import java.io.File
import java.net.URL
import java.security.MessageDigest

object PdfCache {

    private const val PDF_CACHE_DIR = "pdf_cache"
    private const val MAX_CACHE_SIZE_MB = 200L

    // Simple in-memory bitmap cache, cleared when date changes
    private val bitmapCache = mutableMapOf<String, List<Bitmap>>()

    fun getCachedBitmaps(url: String): List<Bitmap>? {
        val cached = bitmapCache[url]
        // Verify bitmaps are still valid (not recycled)
        if (cached != null && cached.all { !it.isRecycled }) {
            return cached
        }
        bitmapCache.remove(url)
        return null
    }

    fun putBitmaps(url: String, bitmaps: List<Bitmap>) {
        bitmapCache[url] = bitmaps
    }

    fun clearBitmapCache() {
        bitmapCache.values.forEach { list ->
            list.forEach { if (!it.isRecycled) it.recycle() }
        }
        bitmapCache.clear()
    }

    fun deleteCachedFile(context: Context, url: String) {
        val file = File(getCacheDir(context), urlToFileName(url))
        if (file.exists()) {
            file.delete()
        }
        bitmapCache.remove(url)
    }

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
