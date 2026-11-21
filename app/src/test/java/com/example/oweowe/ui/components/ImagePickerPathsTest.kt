package com.example.oweowe.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ImagePickerPathsTest {

    @Test
    fun localImagePath_acceptsAbsoluteLocalPaths() {
        val path = "/data/user/0/dk.msd2026.group3.oweowe/files/avatars/a.jpg"

        assertEquals(path, localImagePath(path))
    }

    @Test
    fun localImagePath_rejectsRemoteRelativeAndBlankPaths() {
        assertNull(localImagePath(null))
        assertNull(localImagePath(""))
        assertNull(localImagePath("   "))
        assertNull(localImagePath("avatars/a.jpg"))
        assertNull(localImagePath("content://media/external/images/media/1"))
        assertNull(localImagePath("https://example.com/a.jpg"))
    }
}
