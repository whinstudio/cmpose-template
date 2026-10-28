package com.kturker.multiplatform.core.update

import kotlin.test.Test
import kotlin.test.assertEquals

class RequiredUpdatePathTest {

    @Test
    fun anAllowedImmediateUpdateRunsInTheApp() {
        val path = requiredUpdatePath(updateAvailable = true, updateInProgress = false, immediateAllowed = true)

        assertEquals(RequiredUpdatePath.Immediate, path)
    }

    // Play has no newer build for this install yet (a staged rollout, or not on the track)
    @Test
    fun noUpdateOnPlayOpensTheStore() {
        val path = requiredUpdatePath(updateAvailable = false, updateInProgress = false, immediateAllowed = true)

        assertEquals(RequiredUpdatePath.Store, path)
    }

    @Test
    fun aDisallowedImmediateUpdateOpensTheStore() {
        val path = requiredUpdatePath(updateAvailable = true, updateInProgress = false, immediateAllowed = false)

        assertEquals(RequiredUpdatePath.Store, path)
    }

    @Test
    fun neitherFlagOpensTheStore() {
        val path = requiredUpdatePath(updateAvailable = false, updateInProgress = false, immediateAllowed = false)

        assertEquals(RequiredUpdatePath.Store, path)
    }

    // An immediate update left halfway (the app was closed during it) resumes, whatever the allowed types say
    @Test
    fun anUpdateInProgressResumes() {
        val path = requiredUpdatePath(updateAvailable = false, updateInProgress = true, immediateAllowed = false)

        assertEquals(RequiredUpdatePath.Immediate, path)
    }
}
