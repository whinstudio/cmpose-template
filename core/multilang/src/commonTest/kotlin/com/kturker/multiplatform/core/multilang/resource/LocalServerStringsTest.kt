package com.kturker.multiplatform.core.multilang.resource

import com.kturker.multiplatform.core.multilang.language.AppLanguage
import com.kturker.multiplatform.core.multilang.resource.res.languageResources
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LocalServerStringsTest {

    @Test
    fun theTurkishHintMatchesTheSpec() {
        val turkish = languageResources.getValue(AppLanguage.TR).localServer
        assertEquals(
            "Yerel sunucuya ulaşılamıyor. ./gradlew :server:runLocal çalışıyor mu? " +
                "Emülatördeysen: adb reverse tcp:18001 tcp:18001",
            turkish.hint(
                runCommand = "./gradlew :server:runLocal",
                adbReverseCommand = "adb reverse tcp:18001 tcp:18001"
            )
        )
        assertEquals(
            "Yerel sunucuya ulaşılamıyor. ./gradlew :server:runLocal çalışıyor mu?",
            turkish.hint(runCommand = "./gradlew :server:runLocal", adbReverseCommand = null)
        )
    }

    @Test
    fun everyLanguageCarriesBothCommands() {
        languageResources.forEach { (language, strings) ->
            val android = strings.localServer.hint(runCommand = "RUN", adbReverseCommand = "ADB")
            val ios = strings.localServer.hint(runCommand = "RUN", adbReverseCommand = null)
            assertTrue("RUN" in android && "ADB" in android, "$language: $android")
            assertTrue("RUN" in ios, "$language: $ios")
            assertFalse("ADB" in ios, "$language: $ios")
            listOf(android, ios).forEach { hint ->
                assertFalse('{' in hint || '}' in hint, "$language: $hint")
            }
        }
    }
}
