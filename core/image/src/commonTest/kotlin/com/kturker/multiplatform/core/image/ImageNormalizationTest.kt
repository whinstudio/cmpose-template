package com.kturker.multiplatform.core.image

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class ImageNormalizationTest {

    // Within the backend template's default upload limit (MAX_UPLOAD_BYTES): the server takes every normalized image
    @Test
    fun theUploadLimitIs5Mb() {
        assertEquals(5 * 1024 * 1024, MAX_IMAGE_BYTES)
    }

    @Test
    fun aSmallImageKeepsItsSize() {
        assertEquals(ImageSize(800, 600), targetSize(800, 600))
        assertEquals(ImageSize(MAX_IMAGE_SIDE, MAX_IMAGE_SIDE), targetSize(MAX_IMAGE_SIDE, MAX_IMAGE_SIDE))
    }

    @Test
    fun aLandscapeImageIsScaledByItsWidth() {
        assertEquals(ImageSize(2048, 1536), targetSize(4032, 3024))
    }

    @Test
    fun aPortraitImageIsScaledByItsHeight() {
        assertEquals(ImageSize(1536, 2048), targetSize(3024, 4032))
    }

    @Test
    fun aVeryThinImageKeepsAtLeastOnePixel() {
        assertEquals(ImageSize(2048, 1), targetSize(100_000, 10))
    }

    @Test
    fun theFirstQualityThatFitsIsUsed() {
        val tried = mutableListOf<Int>()
        val result = encodeWithinLimit(maxBytes = 100, qualities = listOf(85, 75, 65)) { quality ->
            tried += quality
            ByteArray(if (quality == 75) 100 else 200)
        }

        assertEquals(listOf(85, 75), tried)
        assertEquals(100, result?.size)
    }

    @Test
    fun theDefaultStartsAt85AndFitsUnder5Mb() {
        val tried = mutableListOf<Int>()
        encodeWithinLimit { quality ->
            tried += quality
            ByteArray(if (quality > 60) MAX_IMAGE_BYTES + 1 else MAX_IMAGE_BYTES)
        }

        assertEquals(listOf(85, 75, 65, 55), tried)
    }

    @Test
    fun whenNothingFitsTheSmallestAttemptIsReturned() {
        val last = ByteArray(150)
        val result = encodeWithinLimit(maxBytes = 100, qualities = listOf(85, 25)) { quality ->
            if (quality == 25) last else ByteArray(300)
        }

        assertSame(last, result)
    }

    @Test
    fun aFailedEncodingGivesNull() {
        assertNull(encodeWithinLimit { null })
    }
}
