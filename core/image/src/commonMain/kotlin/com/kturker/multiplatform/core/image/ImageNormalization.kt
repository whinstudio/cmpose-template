package com.kturker.multiplatform.core.image

/** The largest image the app uploads, in bytes; within the backend template's default upload limit. */
const val MAX_IMAGE_BYTES = 5 * 1024 * 1024

/** The longest side, in pixels, of an image the app uploads; bigger ones are scaled down. */
const val MAX_IMAGE_SIDE = 2048

/** The best JPEG quality, as the encoders count it (0–100). */
const val MAX_JPEG_QUALITY = 100

private const val FIRST_JPEG_QUALITY = 85
private const val LAST_JPEG_QUALITY = 25
private const val JPEG_QUALITY_STEP = 10

/** JPEG qualities tried in turn until the encoded image fits under [MAX_IMAGE_BYTES]: 85, 75 … 25. */
val JPEG_QUALITY_STEPS: List<Int> = (FIRST_JPEG_QUALITY downTo LAST_JPEG_QUALITY step JPEG_QUALITY_STEP).toList()

data class ImageSize(val width: Int, val height: Int)

/** [width] x [height] scaled so the longest side is at most [maxSide], keeping the aspect ratio; never upscales. */
fun targetSize(
    width: Int,
    height: Int,
    maxSide: Int = MAX_IMAGE_SIDE
): ImageSize {
    val longest = maxOf(width, height)
    if (longest <= maxSide) return ImageSize(width, height)
    val scale = maxSide.toDouble() / longest
    return ImageSize(
        width = (width * scale).toInt().coerceIn(1, maxSide),
        height = (height * scale).toInt().coerceIn(1, maxSide)
    )
}

/**
 * Encodes at each of [qualities] in turn and returns the first result of at most [maxBytes]. When none fits,
 * the last (smallest) result is returned, so the caller's size check refuses it; null when encoding fails.
 */
inline fun encodeWithinLimit(
    maxBytes: Int = MAX_IMAGE_BYTES,
    qualities: List<Int> = JPEG_QUALITY_STEPS,
    encode: (quality: Int) -> ByteArray?
): ByteArray? {
    var last: ByteArray? = null
    for (quality in qualities) {
        val encoded = encode(quality) ?: return null
        if (encoded.size <= maxBytes) return encoded
        last = encoded
    }
    return last
}

/**
 * Any image the platform can decode (HEIC from a phone camera included), upright, scaled to [MAX_IMAGE_SIDE]
 * and re-encoded as JPEG, which the backend accepts; null when the bytes are not a decodable image.
 * Runs off the main thread.
 */
expect suspend fun normalizeImageForUpload(bytes: ByteArray): ByteArray?

/** [normalizeImageForUpload] behind an interface, so a view model's tests can do without a platform decoder. */
fun interface ImageNormalizer {
    suspend fun normalize(bytes: ByteArray): ByteArray?
}
