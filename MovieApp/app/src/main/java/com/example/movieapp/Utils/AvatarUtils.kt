package com.example.movieapp.Utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream

object AvatarUtils{
    private const val MAX_DIMENSION_PX = 512
    private const val JPEG_QUALITY = 70

    fun uriToBase64(context: Context, uri: Uri): String? {
        return try {
            val original = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            } ?: return null

            val scaled = scaleDown(original, MAX_DIMENSION_PX)
            val outputStream = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream)
            Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
        } catch (e: Exception){
            null
        }
    }

    fun base64ToBitmap(base64: String): Bitmap? {
        if (base64.isBlank()) return null
        return try {
            val bytes = Base64.decode(base64, Base64.NO_WRAP)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception){
            null
        }
    }

    private fun scaleDown(bitmap: Bitmap, maxDimension: Int): Bitmap{
        val width = bitmap.width
        val height = bitmap.height
        val largestSide = maxOf(width, height)

        if (largestSide <= maxDimension) return bitmap

        val scale = maxDimension.toFloat() / largestSide
        val newWidth = (width*scale).toInt().coerceAtLeast(1)
        val newHeight = (height*scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
}