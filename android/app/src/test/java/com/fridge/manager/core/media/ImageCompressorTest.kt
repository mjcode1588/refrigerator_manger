package com.fridge.manager.core.media

import org.junit.Assert.assertEquals
import org.junit.Test

class ImageCompressorTest {

    @Test
    fun `computeScaledDimensions keeps smaller image unchanged`() {
        val result = ImageCompressor.computeScaledDimensions(800, 600, ImageCompressor.DETAIL_MAX_DIMENSION)
        assertEquals(800 to 600, result)
    }

    @Test
    fun `computeScaledDimensions scales landscape image to detail max 1280`() {
        val result = ImageCompressor.computeScaledDimensions(2560, 1440, ImageCompressor.DETAIL_MAX_DIMENSION)
        assertEquals(1280 to 720, result)
    }

    @Test
    fun `computeScaledDimensions scales portrait image to thumb max 400`() {
        val result = ImageCompressor.computeScaledDimensions(1000, 2000, ImageCompressor.THUMB_MAX_DIMENSION)
        assertEquals(200 to 400, result)
    }
}
