package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.InputStream

object ImageUtils {
    fun uriToBase64(context: Context, uri: Uri, maxDimension: Int = 1024): Pair<String, Long>? {
        var inputStream: InputStream? = null
        return try {
            inputStream = context.contentResolver.openInputStream(uri)
            val original = BitmapFactory.decodeStream(inputStream) ?: return null

            val width = original.width
            val height = original.height

            val scale = if (width > maxDimension || height > maxDimension) {
                val ratio = width.toFloat() / height.toFloat()
                if (ratio > 1) {
                    Pair(maxDimension, (maxDimension / ratio).toInt())
                } else {
                    Pair((maxDimension * ratio).toInt(), maxDimension)
                }
            } else {
                Pair(width, height)
            }

            val resized = Bitmap.createScaledBitmap(original, scale.first, scale.second, true)
            val outputStream = ByteArrayOutputStream()
            resized.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            val bytes = outputStream.toByteArray()
            val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            Pair(base64, bytes.size.toLong())
        } catch (e: Exception) {
            null
        } finally {
            inputStream?.close()
        }
    }
}
