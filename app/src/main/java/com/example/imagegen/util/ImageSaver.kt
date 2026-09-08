package com.example.imagegen.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream

/**
 * 图片保存工具（保存到相册）
 */
object ImageSaver {
    
    fun saveToGallery(context: Context, bitmap: Bitmap): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveToMediaStore(context, bitmap)
            } else {
                saveToExternalStorage(context, bitmap)
            }
        } catch (e: Exception) {
            false
        }
    }
    
    private fun saveToMediaStore(context: Context, bitmap: Bitmap): Boolean {
        val fileName = "AI_${System.currentTimeMillis()}.png"
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/AI图片生成器")
        }
        val uri = context.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ) ?: return false
        
        return context.contentResolver.openOutputStream(uri)?.use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            true
        } ?: false
    }
    
    private fun saveToExternalStorage(context: Context, bitmap: Bitmap): Boolean {
        val dir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            "AI图片生成器"
        )
        if (!dir.exists()) {
            dir.mkdirs()
        }
        val file = File(dir, "AI_${System.currentTimeMillis()}.png")
        return FileOutputStream(file).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            MediaScannerConnection.scanFile(
                context,
                arrayOf(file.absolutePath),
                arrayOf("image/png"),
                null
            )
            true
        }
    }
}
