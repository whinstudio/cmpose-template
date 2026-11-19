package convention

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.register

private const val CHECK_LEGAL_URLS = "checkLegalUrls"
private const val ALLOW_EMPTY_PROPERTY = "cmpose.allowEmptyLegalUrls"
private const val ALLOW_EMPTY_ENV = "CMPOSE_ALLOW_EMPTY_LEGAL_URLS"
private val LEGAL_URL_NAMES = listOf("PRIVACY_POLICY_URL", "TERMS_URL")

/**
 * The names in [LEGAL_URL_NAMES] whose value in [source] is blank (`""` or whitespace only).
 *
 * A constant the pattern does not find counts as filled, since it may be written another way, and comments or TODO
 * text never count as empty. External tools may apply the same pattern to reach the same verdict, so it must not
 * change.
 */
fun emptyLegalUrls(source: String): List<String> = LEGAL_URL_NAMES.filter { name ->
    val pattern = Regex("""const\s+val\s+$name\s*(?::\s*String\s*)?=\s*"([^"]*)"""")
    val value = pattern.find(source)?.groupValues?.get(1)
    value != null && value.isBlank()
}

/**
 * Fails while `PRIVACY_POLICY_URL` or `TERMS_URL` in core/about's `LegalUrls.kt` is empty: both stores ask for a
 * privacy policy URL. With `-Pcmpose.allowEmptyLegalUrls=true` or `CMPOSE_ALLOW_EMPTY_LEGAL_URLS=true` in the
 * environment it logs a warning instead.
 */
abstract class CheckLegalUrlsTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sources: ConfigurableFileCollection

    @get:Input
    abstract val allowEmpty: Property<Boolean>

    @TaskAction
    fun check() {
        val empty = sources.files.flatMap { emptyLegalUrls(it.readText()) }.distinct()
        if (empty.isEmpty()) return
        val problem = "Set PRIVACY_POLICY_URL and TERMS_URL in LegalUrls.kt before a store build " +
            "(empty: ${empty.joinToString()})."
        if (allowEmpty.get()) {
            logger.warn("$problem The check was skipped by the $ALLOW_EMPTY_PROPERTY / $ALLOW_EMPTY_ENV override.")
        } else {
            throw GradleException(
                "$problem To build anyway, pass -P$ALLOW_EMPTY_PROPERTY=true or set $ALLOW_EMPTY_ENV=true in the " +
                    "environment."
            )
        }
    }
}

/**
 * Registers `checkLegalUrls` on the root project once and returns it. Store builds (Android `bundleRelease`, the
 * Xcode archive) depend on it; `assembleRelease` and an Xcode `build` do not.
 */
fun Project.registerLegalUrlsCheck(): TaskProvider<Task> {
    val root = rootProject
    if (CHECK_LEGAL_URLS !in root.tasks.names) {
        root.tasks.register<CheckLegalUrlsTask>(CHECK_LEGAL_URLS) {
            group = "verification"
            description = "Fails while a legal URL in core/about's LegalUrls.kt is empty."
            // Matched by name, so a renamed package still works; the files are read when the task runs
            sources.from(
                root.fileTree(root.file("core/about/src/commonMain/kotlin")) { include("**/LegalUrls.kt") }
            )
            // Either escape works: the Gradle property, or the environment variable (easier to set around an
            // `xcodebuild archive`, whose script phase inherits the environment)
            val fromProperty = root.providers.gradleProperty(ALLOW_EMPTY_PROPERTY).map { it.trim().toBoolean() }
            val fromEnv = root.providers.environmentVariable(ALLOW_EMPTY_ENV).map { it.trim().toBoolean() }
            allowEmpty.set(fromProperty.orElse(false).zip(fromEnv.orElse(false)) { property, env -> property || env })
        }
    }
    return root.tasks.named(CHECK_LEGAL_URLS)
}
