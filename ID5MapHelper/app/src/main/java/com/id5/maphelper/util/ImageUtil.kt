package com.id5.maphelper.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 图片处理工具类
 * 负责将用户选择的图片保存到应用私有目录
 */
object ImageUtil {

    /**
     * 将Uri图片保存到应用私有目录
     * @return 保存后的文件路径
     */
    fun saveImageToInternalStorage(context: Context, uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "map_$timeStamp.jpg"
            val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "maps")
            if (!dir.exists()) dir.mkdirs()

            val file = File(dir, fileName)
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            outputStream.flush()
            outputStream.close()

            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 从路径加载Bitmap
     */
    fun loadBitmap(path: String, reqWidth: Int = 0, reqHeight: Int = 0): Bitmap? {
        if (path.isEmpty()) return null
        return try {
            if (reqWidth > 0 && reqHeight > 0) {
                val options = BitmapFactory.Options()
                options.inJustDecodeBounds = true
                BitmapFactory.decodeFile(path, options)
                options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
                options.inJustDecodeBounds = false
                BitmapFactory.decodeFile(path, options)
            } else {
                BitmapFactory.decodeFile(path)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 计算图片采样率
     */
    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    /**
     * 删除图片文件
     */
    fun deleteImage(path: String): Boolean {
        return try {
            val file = File(path)
            if (file.exists()) file.delete() else true
        } catch (e: Exception) {
            false
        }
    }
}
