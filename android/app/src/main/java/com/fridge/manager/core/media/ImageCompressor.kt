package com.fridge.manager.core.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

data class CompressedItemPhotos(
    val detailBytes: ByteArray,
    val thumbBytes: ByteArray,
)

object ImageCompressor {
    const val DETAIL_MAX_DIMENSION = 1280
    const val DETAIL_MAX_BYTES = 500 * 1024
    const val THUMB_MAX_DIMENSION = 400
    const val THUMB_MAX_BYTES = 100 * 1024

    fun computeScaledDimensions(
        width: Int,
        height: Int,
        maxDimension: Int,
    ): Pair<Int, Int> {
        if (width <= 0 || height <= 0 || maxDimension <= 0) return 1 to 1
        val longest = max(width, height)
        if (longest <= maxDimension) return width to height
        val scale = maxDimension.toDouble() / longest.toDouble()
        val targetW = (width * scale).roundToInt().coerceAtLeast(1)
        val targetH = (height * scale).roundToInt().coerceAtLeast(1)
        return targetW to targetH
    }

    fun compressFromUri(context: Context, uri: Uri): CompressedItemPhotos {
        val rawBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: error("이미지를 읽을 수 없습니다.")
        val decoded = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size)
            ?: error("지원하지 않는 이미지 형식입니다.")
        return try {
            val detail = compressBitmap(decoded, DETAIL_MAX_DIMENSION, DETAIL_MAX_BYTES)
            val thumb = compressBitmap(decoded, THUMB_MAX_DIMENSION, THUMB_MAX_BYTES)
            CompressedItemPhotos(detailBytes = detail, thumbBytes = thumb)
        } finally {
            decoded.recycle()
        }
    }

    private fun compressBitmap(
        source: Bitmap,
        maxDimension: Int,
        maxBytes: Int,
    ): ByteArray {
        val (targetW, targetH) = computeScaledDimensions(source.width, source.height, maxDimension)
        val scaled = if (targetW == source.width && targetH == source.height) {
            source
        } else {
            Bitmap.createScaledBitmap(source, targetW, targetH, true)
        }
        try {
            @Suppress("DEPRECATION")
            val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Bitmap.CompressFormat.WEBP_LOSSY
            } else {
                Bitmap.CompressFormat.WEBP
            }
            var quality = 85
            var bestBytes = ByteArray(0)
            while (quality >= 35) {
                val out = ByteArrayOutputStream()
                scaled.compress(format, quality, out)
                bestBytes = out.toByteArray()
                if (bestBytes.size <= maxBytes) {
                    return bestBytes
                }
                quality -= 10
            }
            return bestBytes
        } finally {
            if (scaled !== source) {
                scaled.recycle()
            }
        }
    }
}
