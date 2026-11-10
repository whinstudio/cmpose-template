package com.kturker.multiplatform.core.multilang.resource

import com.kturker.multiplatform.core.multilang.resource.res.languageResources
import kotlin.test.Test
import kotlin.test.assertTrue

class MonetizationStringsTest {

    private val templates: List<Pair<String, (StringResourcesUiModel) -> String>> = listOf(
        "{price}" to { it.paywallPriceWeekly },
        "{price}" to { it.paywallPriceMonthly },
        "{price}" to { it.paywallPriceAnnual },
        "{price}" to { it.paywallPriceLifetime },
        "{price}" to { it.paywallPricePerMonth },
        "{percent}" to { it.paywallSave },
        "{trial}" to { it.paywallFreeTrial },
        "{coins}" to { it.adsCoins },
        "{reason}" to { it.adsFailed }
    )

    @Test
    fun everyLanguageKeepsThePlaceholders() {
        languageResources.forEach { (language, strings) ->
            templates.forEach { (placeholder, read) ->
                assertTrue(placeholder in read(strings), "$language: ${read(strings)}")
            }
        }
    }

    @Test
    fun everyLanguageFillsTheNewStrings() {
        languageResources.forEach { (language, strings) ->
            listOf(strings.paywallNotNow, strings.paywallRenewalTerms, strings.purchaseErrorFailed, strings.adsTestNote)
                .forEach { assertTrue(it.isNotBlank(), "$language") }
        }
    }
}
