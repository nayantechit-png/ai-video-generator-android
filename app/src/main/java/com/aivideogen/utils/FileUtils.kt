package com.aivideogen.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FileUtils {

    fun getVideoOutputDir(context: Context): File {
        val dir = File(context.filesDir, "videos")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getImagesDir(context: Context): File {
        val dir = File(context.filesDir, "images")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getTempDir(context: Context): File {
        val dir = File(context.cacheDir, "temp")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun saveBase64Image(context: Context, base64: String, prefix: String = "img"): File {
        val bytes = Base64.decode(base64, Base64.DEFAULT)
        val file = File(getImagesDir(context), "${prefix}_${timestamp()}.png")
        file.writeBytes(bytes)
        return file
    }

    fun saveBitmapToFile(context: Context, bitmap: Bitmap, prefix: String = "img"): File {
        val file = File(getImagesDir(context), "${prefix}_${timestamp()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        return file
    }

    fun copyUriToFile(context: Context, uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val ext = context.contentResolver.getType(uri)?.substringAfterLast("/") ?: "jpg"
            val file = File(getImagesDir(context), "upload_${timestamp()}.$ext")
            file.outputStream().use { out -> inputStream.copyTo(out) }
            file
        } catch (e: Exception) {
            null
        }
    }

    fun loadBitmapFromFile(path: String): Bitmap? {
        return try {
            BitmapFactory.decodeFile(path)
        } catch (e: Exception) {
            null
        }
    }

    fun resizeBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val ratio = minOf(
            maxWidth.toFloat() / bitmap.width,
            maxHeight.toFloat() / bitmap.height
        )
        val newWidth  = (bitmap.width  * ratio).toInt()
        val newHeight = (bitmap.height * ratio).toInt()
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    fun getFileSizeKB(path: String): Long = File(path).length() / 1024

    fun formatFileSize(bytes: Long): String = when {
        bytes < 1024       -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else               -> "${"%.1f".format(bytes / (1024.0 * 1024.0))} MB"
    }

    fun cleanTempFiles(context: Context) {
        getTempDir(context).listFiles()?.forEach { it.delete() }
    }

    private fun timestamp(): String =
        SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
}
