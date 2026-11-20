package com.kturker.multiplatform.core.multilang.resource

import com.kturker.multiplatform.core.multilang.language.AppLanguage
import com.kturker.multiplatform.core.multilang.resource.res.fallbackResources
import com.kturker.multiplatform.core.multilang.resource.res.languageResources
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SharedSpacesWarningTest {

    private val english = fallbackResources.account

    @Test
    fun oneSpaceUsesTheSingular() {
        assertEquals(
            "1 space you share with others will be deleted too, and its members lose access.",
            english.sharedSpacesDeleteWarning(1)
        )
    }

    @Test
    fun otherCountsUseThePlural() {
        assertEquals(
            "3 spaces you share with others will be deleted too, and their members lose access.",
            english.sharedSpacesDeleteWarning(3)
        )
        assertTrue(english.sharedSpacesDeleteWarning(12).startsWith("12 spaces"))
    }

    @Test
    fun turkishKeepsOneFormForEveryCount() {
        val turkish = languageResources.getValue(AppLanguage.TR).account

        assertEquals(
            "Paylaştığın 1 alan da silinecek; üyeleri erişimini kaybeder.",
            turkish.sharedSpacesDeleteWarning(1)
        )
        assertEquals(
            "Paylaştığın 4 alan da silinecek; üyeleri erişimini kaybeder.",
            turkish.sharedSpacesDeleteWarning(4)
        )
    }

    // Every language puts the number in both forms, and no placeholder is left in the text
    @Test
    fun everyLanguageFillsInTheCount() {
        languageResources.forEach { (language, strings) ->
            listOf(1, 2, 7).forEach { count ->
                val text = strings.account.sharedSpacesDeleteWarning(count)
                assertTrue(text.contains(count.toString()), "$language $count: $text")
                assertFalse(text.contains("{count}"), "$language $count: $text")
            }
        }
    }

    @Test
    fun oneJoinedSpaceUsesTheSingular() {
        assertEquals(
            "Content you added to 1 space owned by someone else stays there and is no longer linked to you.",
            english.joinedSpacesKeptWarning(1)
        )
    }

    @Test
    fun otherJoinedCountsUseThePlural() {
        assertEquals(
            "Content you added to 2 spaces owned by others stays there and is no longer linked to you.",
            english.joinedSpacesKeptWarning(2)
        )
    }

    @Test
    fun turkishKeepsOneJoinedFormForEveryCount() {
        val turkish = languageResources.getValue(AppLanguage.TR).account

        assertEquals(
            "Başkasına ait 1 alana eklediğin içerik orada kalır ve artık seninle ilişkilendirilmez.",
            turkish.joinedSpacesKeptWarning(1)
        )
        assertEquals(
            "Başkasına ait 3 alana eklediğin içerik orada kalır ve artık seninle ilişkilendirilmez.",
            turkish.joinedSpacesKeptWarning(3)
        )
    }

    @Test
    fun everyLanguageFillsInTheJoinedCount() {
        languageResources.forEach { (language, strings) ->
            listOf(1, 2, 7).forEach { count ->
                val text = strings.account.joinedSpacesKeptWarning(count)
                assertTrue(text.contains(count.toString()), "$language $count: $text")
                assertFalse(text.contains("{count}"), "$language $count: $text")
            }
        }
    }
}
