package com.kturker.multiplatform.core.multilang.resource

import com.kturker.multiplatform.core.multilang.language.AppLanguage
import com.kturker.multiplatform.core.multilang.resource.res.languageResources

/**
 * The debug build's "local server unreachable" strip, one instance per language ([forLanguage]), kept out of
 * [StringResourcesUiModel] so an app without the backend has none of them. No default values: a language that misses
 * a string does not compile. The commands themselves are never translated: [hint] takes them as parameters.
 */
data class LocalServerStrings(
    /** The server cannot be reached; `{run}` stands for the command that starts it. */
    val unreachable: String,
    /** The Android emulator's extra step; `{adb}` stands for the `adb reverse` command. */
    val onEmulator: String
) {
    companion object {
        private val EN = LocalServerStrings(
            unreachable = "Can't reach the local server. Is {run} running?",
            onEmulator = "On an emulator: {adb}"
        )

        private val TR = LocalServerStrings(
            unreachable = "Yerel sunucuya ulaşılamıyor. {run} çalışıyor mu?",
            onEmulator = "Emülatördeysen: {adb}"
        )

        private val ES = LocalServerStrings(
            unreachable = "No se puede conectar con el servidor local. ¿Está en marcha {run}?",
            onEmulator = "En un emulador: {adb}"
        )

        private val IT = LocalServerStrings(
            unreachable = "Impossibile raggiungere il server locale. {run} è in esecuzione?",
            onEmulator = "Su un emulatore: {adb}"
        )

        private val FR = LocalServerStrings(
            unreachable = "Impossible de joindre le serveur local. {run} est-il lancé ?",
            onEmulator = "Sur un émulateur : {adb}"
        )

        /** The strings for an [AppLanguage] code; English for any other. */
        fun forLanguage(code: String): LocalServerStrings = when (code) {
            AppLanguage.TR.code -> TR
            AppLanguage.ES.code -> ES
            AppLanguage.IT.code -> IT
            AppLanguage.FR.code -> FR
            else -> EN
        }
    }
}

/**
 * The local server strip's strings in the language of these app strings, so they follow the in-app language picker
 * as [StringResourcesUiModel] does: `LocalStringResources.current.localServer`.
 */
val StringResourcesUiModel.localServer: LocalServerStrings
    get() = LocalServerStrings.forLanguage(languageCode())

// The language whose strings these are (LanguageManager hands out the objects of resource/res); English otherwise
private fun StringResourcesUiModel.languageCode(): String =
    languageResources.entries.firstOrNull { it.value === this }?.key?.code ?: AppLanguage.EN.code

private const val RUN = "{run}"
private const val ADB = "{adb}"

/**
 * The strip's text: [LocalServerStrings.unreachable] with [runCommand], followed by [LocalServerStrings.onEmulator]
 * with [adbReverseCommand] when there is one (Android; iOS passes `null`).
 */
fun LocalServerStrings.hint(
    runCommand: String,
    adbReverseCommand: String?
): String {
    val text = unreachable.replace(RUN, runCommand)
    return if (adbReverseCommand == null) text else text + " " + onEmulator.replace(ADB, adbReverseCommand)
}
