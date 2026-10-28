package com.kturker.multiplatform.core.multilang.resource

import com.kturker.multiplatform.core.multilang.language.AppLanguage
import com.kturker.multiplatform.core.multilang.resource.res.fallbackResources
import com.kturker.multiplatform.core.multilang.resource.res.languageResources
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class SharingStringsTest {

    private val english = fallbackResources.sharing
    private val turkish = languageResources.getValue(AppLanguage.TR).sharing

    @Test
    fun theShareTextNamesTheAppAndCarriesTheLink() {
        assertEquals(
            "Join my list in Groceries: myapp://invite/abc",
            english.shareInviteText(appName = "Groceries", url = "myapp://invite/abc")
        )
        assertEquals(
            "Groceries uygulamasındaki listeme katıl: myapp://invite/abc",
            turkish.shareInviteText(appName = "Groceries", url = "myapp://invite/abc")
        )
    }

    @Test
    fun thePendingInviteSaysHowManyDaysAreLeft() {
        assertEquals("Days left: 3", english.inviteDaysLeftText(3))
        assertEquals("Kalan gün: 3", turkish.inviteDaysLeftText(3))
    }

    @Test
    fun theInviteMessageNamesTheInviterWhenThereIsOne() {
        assertEquals("Ada invites you to join Home.", english.inviteMessage(inviterName = "Ada", spaceName = "Home"))
        assertEquals("You are invited to join Home.", english.inviteMessage(inviterName = null, spaceName = "Home"))
        assertEquals("You are invited to join Home.", english.inviteMessage(inviterName = " ", spaceName = "Home"))
    }

    // Every language fills in every placeholder, and none is left in the text
    @Test
    fun everyLanguageFillsInThePlaceholders() {
        languageResources.forEach { (language, all) ->
            val strings = all.sharing
            val texts = listOf(
                strings.shareInviteText(appName = "APPNAME", url = "URL-1"),
                strings.inviteMessage(inviterName = "INVITER", spaceName = "SPACE"),
                strings.inviteMessage(inviterName = null, spaceName = "SPACE"),
                strings.inviteRoleText("ROLE"),
                strings.inviteDaysLeftText(7)
            )
            assertTrue(texts[0].contains("APPNAME") && texts[0].contains("URL-1"), "$language: ${texts[0]}")
            assertTrue(texts[1].contains("INVITER") && texts[1].contains("SPACE"), "$language: ${texts[1]}")
            assertTrue(texts[2].contains("SPACE"), "$language: ${texts[2]}")
            assertTrue(texts[3].contains("ROLE"), "$language: ${texts[3]}")
            assertTrue(texts[4].contains("7"), "$language: ${texts[4]}")
            texts.forEach { assertFalse(it.contains('{') || it.contains('}'), "$language: $it") }
            assertTrue(strings.retry.isNotBlank(), "$language: retry")
            assertTrue(strings.unnamedMember.isNotBlank(), "$language: unnamedMember")
        }
    }

    // The strings follow the app strings' language (the in-app picker's choice), not the device's
    @Test
    fun theSharingStringsFollowTheAppStringsLanguage() {
        languageResources.forEach { (language, all) ->
            assertEquals(SharingStrings.forLanguage(language.code), all.sharing, "$language")
            assertEquals(AccountStrings.forLanguage(language.code), all.account, "$language")
        }
        assertNotEquals(english, turkish)
        assertNotEquals(fallbackResources.account, languageResources.getValue(AppLanguage.TR).account)
    }

    // An unknown code, or app strings that are none of the languages' own, fall back to English
    @Test
    fun anythingElseGetsTheEnglishStrings() {
        assertEquals(english, SharingStrings.forLanguage("de"))
        assertEquals(english, SharingStrings.forLanguage(AppLanguage.SYSTEM.code))
        assertEquals(fallbackResources.account, AccountStrings.forLanguage("de"))
        val custom = languageResources.getValue(AppLanguage.TR).copy(settingsTitle = "Custom")
        assertEquals(english, custom.sharing)
        assertEquals(fallbackResources.account, custom.account)
    }
}
