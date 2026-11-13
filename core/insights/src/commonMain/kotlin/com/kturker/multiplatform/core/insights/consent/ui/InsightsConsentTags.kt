package com.kturker.multiplatform.core.insights.consent.ui

/**
 * Test tags on [InsightsConsentDialog]'s buttons. Automated walkthroughs (Maestro) tap these to answer the prompt
 * whatever the app's language, so the values must stay as they are.
 */
object InsightsConsentTags {
    /** The button that allows Analytics. */
    const val ALLOW = "insights_consent_allow"

    /** The button that keeps Analytics off. */
    const val DENY = "insights_consent_deny"
}
