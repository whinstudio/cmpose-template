package com.kturker.multiplatform.core.api

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assume
import java.io.File
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Checks the client's routes and models against the backend's API description (OpenAPI 3), which the
 * server's `writeOpenApi` task writes to `server/build/openapi/openapi.json`; the build hands its path over
 * as the `cmpose.openapi` system property. Skipped when there is no such file (no server written yet),
 * unless the build says the server is there (`cmpose.openapi.required=true`): then a missing file fails.
 * Routes come from [ApiRoutes] itself, the HTTP method of each from the client's call sites (the features
 * the build names in the `cmpose.clientSources` system property, read by [clientCalls]), and which models are
 * sent or received from the operations, so a new route, call or endpoint needs no edit here; a new schema
 * needs its model in [MODELS].
 */
@OptIn(ExperimentalSerializationApi::class)
class ApiContractTest {

    private val document: JsonObject by lazy { loadDocument() }

    /** The document's operations by normalized path: HTTP method → operation. */
    private val operations: Map<String, Map<String, JsonObject>> by lazy {
        document.getValue("paths").jsonObject.entries.associate { (path, item) ->
            normalized(path) to item.jsonObject.filterKeys { it in HTTP_METHODS }.mapValues { it.value.jsonObject }
        }
    }

    /** Every path [ApiRoutes] builds, each with the methods the API description has for it. */
    private val clientRoutes: Map<String, Set<String>> by lazy {
        clientPaths().associateWith { operations[it]?.keys.orEmpty() }
    }

    @Test
    fun everyRouteHasAnOperationAndEveryPathARoute() {
        val unserved = clientRoutes.filterValues { it.isEmpty() }.keys
        val problems = unserved.map { "$it: no operation in the API description" } +
            (operations.keys - clientRoutes.keys).map { "$it: in the API description, not in ApiRoutes" }
        assertTrue(problems.isEmpty(), "Routes client and server disagree on:\n" + problems.joinToString("\n"))
    }

    @Test
    fun everyClientCallHasItsMethod() {
        // First, so that without the API description the test is skipped like the others
        val served = operations
        val problems = mutableListOf<String>()
        val calls = sortedSetOf<Pair<String, String>>(compareBy({ it.second }, { it.first }))
        val dirs = clientSourceDirs()
        dirs.flatMap(::mainSources).forEach { file ->
            val scan = clientCalls(file.readText())
            calls += scan.calls
            scan.unread.forEach { problems += "${file.name}: an ApiRoutes use not in a client.<method>(...) call: $it" }
        }
        if (calls.isEmpty()) problems += "no client.<method>(ApiRoutes...) call found in ${dirs.joinToString()}"
        calls.forEach { (method, path) ->
            val methods = served[path]?.keys.orEmpty()
            if (method !in methods) {
                val has = methods.map { it.uppercase() }.sorted().joinToString().ifEmpty { "no operation" }
                problems += "${method.uppercase()} $path: the API description has $has"
            }
        }
        assertTrue(problems.isEmpty(), "Client calls the server does not serve:\n" + problems.joinToString("\n"))
    }

    @Test
    fun scanReadsConstantAndBuilderRoutes() {
        val scan = clientCalls(
            """
            request { client.get(ApiRoutes.ME) }
            request { client.patch(
                ApiRoutes.member(spaceId, userId)) { json(x) } }
            // ApiRoutes.SPACES in a comment
            /** [ApiRoutes.SPACES] in KDoc */
            val url = "https://example.com" // ApiRoutes.SPACES after a string
            """.trimIndent()
        )
        assertEquals(listOf("get" to "/me", "patch" to "/spaces/{}/members/{}"), scan.calls)
        assertEquals(emptyList(), scan.unread)
    }

    @Test
    fun unreadUseFailsTheScan() {
        val scan = clientCalls("val route = ApiRoutes.PROFILE\nrequest { client.put(route) }")
        assertEquals(listOf("val route = ApiRoutes.PROFILE"), scan.unread)
    }

    @Test
    fun everyModelMatchesItsSchema() {
        val problems = mutableListOf<String>()
        schemaUses().forEach { (name, directions) ->
            val descriptor = MODELS[name]
            val schema = schemas[name]?.jsonObject
            when {
                descriptor == null -> problems += "$name: an operation uses it, MODELS has no model for it"
                schema == null -> problems += "$name: not in the API description"
                else -> directions.forEach { response -> check(name, descriptor, schema, response, problems) }
            }
        }
        assertTrue(problems.isEmpty(), "Models that do not match the server:\n" + problems.joinToString("\n"))
    }

    /**
     * The schemas the operations name in their request bodies (false) and responses (true), errors included.
     * Schemas only nested in another (Role inside Member) are checked through the model that holds them.
     */
    private fun schemaUses(): Map<String, Set<Boolean>> {
        val uses = mutableMapOf<String, MutableSet<Boolean>>()
        operations.values.flatMap { it.values }.forEach { operation ->
            operation["requestBody"]?.let(::schemaRefs)?.forEach { uses.getOrPut(it) { mutableSetOf() } += false }
            operation["responses"]?.let(::schemaRefs)?.forEach { uses.getOrPut(it) { mutableSetOf() } += true }
        }
        return uses
    }

    /** The schema names [element] refers to, through shared responses and request bodies in `components`. */
    private fun schemaRefs(element: JsonElement): Set<String> = when (element) {
        is JsonArray -> element.flatMapTo(mutableSetOf(), ::schemaRefs)
        is JsonObject -> {
            val ref = element["\$ref"]?.jsonPrimitive?.content
            when {
                ref == null -> element.values.flatMapTo(mutableSetOf(), ::schemaRefs)
                ref.startsWith(SCHEMA_REF) -> setOf(ref.removePrefix(SCHEMA_REF))
                else -> schemaRefs(component(ref))
            }
        }
        else -> emptySet()
    }

    /** `#/components/<kind>/<name>` */
    private fun component(ref: String): JsonObject {
        val path = ref.removePrefix("#/components/")
        val kind = path.substringBefore("/")
        val name = path.substringAfter("/")
        return document.getValue("components").jsonObject[kind]?.jsonObject?.get(name)?.jsonObject
            ?: error("Unresolved \$ref $ref")
    }

    /** [ApiRoutes]' constants, and its functions called with a path parameter for each argument. */
    private fun clientPaths(): Set<String> =
        (routeConstants().values + routeBuilders().map(::buildRoute)).mapTo(mutableSetOf()) { normalized("/$it") }

    /** The feature directories the build names; failing, not skipping, when it names none. */
    private fun clientSourceDirs(): List<File> {
        val dirs = System.getProperty(CLIENT_SOURCES_PROPERTY).orEmpty()
            .split(File.pathSeparator).filter { it.isNotBlank() }.map(::File)
        if (dirs.isEmpty()) fail("ApiContractTest: no client sources (-D$CLIENT_SOURCES_PROPERTY not set)")
        return dirs
    }

    /** The Kotlin files in [dir]'s main source sets (`src/commonMain`, `src/androidMain`, ...), not its tests. */
    private fun mainSources(dir: File): List<File> = dir.walkTopDown()
        .onEnter { it.name != "build" }
        .filter { it.isFile && it.extension == "kt" }
        .filter { MAIN_SOURCE.containsMatchIn(it.relativeTo(dir).invariantSeparatorsPath) }
        .sortedBy { it.path }
        .toList()

    private val schemas: JsonObject
        get() = document.getValue("components").jsonObject.getValue("schemas").jsonObject

    /**
     * Compares one model ([descriptor]) with its [schema] at [where], recursing into lists and nested
     * objects. The schema's required fields must all be in the model. In a [response] a field the server
     * may send as null must be nullable in the model, and one it may leave out must have a default; in a
     * request a required field must have no default, as the client's Json does not encode defaults.
     */
    private fun check(
        where: String,
        descriptor: SerialDescriptor,
        schema: JsonObject,
        response: Boolean,
        problems: MutableList<String>,
    ) {
        val resolved = resolve(schema)
        val modelType = jsonType(descriptor)
        val schemaType = resolved["type"]?.jsonPrimitive?.content
        if (modelType != schemaType) {
            problems += "$where: the model is $modelType, the schema is $schemaType"
            return
        }
        when (descriptor.kind) {
            SerialKind.ENUM -> {
                val modelValues = (0 until descriptor.elementsCount).map(descriptor::getElementName).toSet()
                val schemaValues = resolved["enum"]?.jsonArray?.map { it.jsonPrimitive.content }?.toSet()
                if (modelValues != schemaValues) problems += "$where: values $modelValues, schema has $schemaValues"
            }
            StructureKind.LIST -> {
                val items = resolved.getValue("items").jsonObject
                check("$where[]", descriptor.getElementDescriptor(0), items, response, problems)
            }
            StructureKind.CLASS -> checkObject(where, descriptor, resolved, response, problems)
            PrimitiveKind.INT -> if (resolved["format"]?.jsonPrimitive?.content == "int64") {
                problems += "$where: the schema is int64, the model is Int"
            }
            else -> Unit
        }
    }

    private fun checkObject(
        where: String,
        descriptor: SerialDescriptor,
        schema: JsonObject,
        response: Boolean,
        problems: MutableList<String>,
    ) {
        val properties = schema["properties"]?.jsonObject ?: JsonObject(emptyMap())
        val required = schema["required"]?.jsonArray?.map { it.jsonPrimitive.content }.orEmpty().toSet()
        val fields = (0 until descriptor.elementsCount).map(descriptor::getElementName)
        fields.forEachIndexed { index, field ->
            val property = properties[field]?.jsonObject
            if (property == null) {
                problems += "$where.$field: not in the schema"
                return@forEachIndexed
            }
            val element = descriptor.getElementDescriptor(index)
            val optional = descriptor.isElementOptional(index)
            if (response && property.isNullable() && !element.isNullable) {
                problems += "$where.$field: the server may send null, the model is not nullable"
            }
            if (response && field !in required && !optional) {
                problems += "$where.$field: the server may leave it out, the model has no default"
            }
            if (!response && field in required && optional) {
                problems += "$where.$field: required by the server, the model has a default and may not send it"
            }
            check("$where.$field", element, property, response, problems)
        }
        (required - fields.toSet()).forEach { problems += "$where.$it: required by the server, not in the model" }
    }

    /** Follows a `$ref` to `#/components/schemas/<name>`. */
    private fun resolve(schema: JsonObject): JsonObject {
        val ref = schema["\$ref"]?.jsonPrimitive?.content ?: return schema
        val name = ref.removePrefix(SCHEMA_REF)
        return resolve(schemas[name]?.jsonObject ?: error("Unresolved \$ref $ref"))
    }

    private fun JsonObject.isNullable() = resolve(this)["nullable"]?.jsonPrimitive?.booleanOrNull == true

    private fun jsonType(descriptor: SerialDescriptor): String = when (descriptor.kind) {
        PrimitiveKind.STRING, SerialKind.ENUM -> "string"
        PrimitiveKind.INT, PrimitiveKind.LONG -> "integer"
        PrimitiveKind.BOOLEAN -> "boolean"
        StructureKind.LIST -> "array"
        StructureKind.CLASS -> "object"
        else -> descriptor.kind.toString()
    }

    private fun loadDocument(): JsonObject {
        val path = System.getProperty(OPENAPI_PROPERTY)
        val file = path?.let(::File)
        if (file == null || !file.isFile) {
            val note = "no OpenAPI document at ${path ?: "(-D$OPENAPI_PROPERTY not set)"}"
            if (System.getProperty(REQUIRED_PROPERTY).toBoolean()) {
                fail("ApiContractTest: $note, although the server is part of this build (run its writeOpenApi task)")
            }
            // The skip reason travels in the assumption; Gradle reports it with the skipped test
            Assume.assumeTrue("ApiContractTest skipped: $note (run the server's writeOpenApi task)", false)
        }
        return Json.parseToJsonElement(file!!.readText()).jsonObject
    }

    private companion object {
        const val OPENAPI_PROPERTY = "cmpose.openapi"
        const val REQUIRED_PROPERTY = "cmpose.openapi.required"
        const val CLIENT_SOURCES_PROPERTY = "cmpose.clientSources"
        const val SCHEMA_REF = "#/components/schemas/"
        val HTTP_METHODS = setOf("get", "put", "post", "delete", "patch", "head", "options", "trace")
        val MAIN_SOURCE = Regex("(^|/)src/[^/]*Main/")

        /** Each schema with its model; whether it is sent or received comes from the operations that use it. */
        val MODELS: Map<String, SerialDescriptor> = mapOf(
            "Role" to Role.serializer().descriptor,
            "InviteState" to InviteState.serializer().descriptor,
            "Space" to Space.serializer().descriptor,
            "SpaceDraft" to SpaceDraft.serializer().descriptor,
            "Member" to Member.serializer().descriptor,
            "RoleChange" to RoleChange.serializer().descriptor,
            "InviteRequest" to InviteRequest.serializer().descriptor,
            "Invite" to InviteLink.serializer().descriptor,
            "PendingInvite" to PendingInvite.serializer().descriptor,
            "InviteToken" to InviteToken.serializer().descriptor,
            "InvitePreview" to InvitePreview.serializer().descriptor,
            "Me" to Me.serializer().descriptor,
            "ProfileUpdate" to Profile.serializer().descriptor,
            "File" to FileInfo.serializer().descriptor,
            "Error" to ErrorBody.serializer().descriptor,
        )
    }
}

/**
 * What [clientCalls] read from a source: its calls as (lower-case HTTP method, normalized path), and the lines
 * with an [ApiRoutes] use it could not read.
 */
internal data class ClientScan(val calls: List<Pair<String, String>>, val unread: List<String>)

/**
 * Reads the API calls in Kotlin [source]: `client.<method>(` followed, line breaks allowed, by `ApiRoutes.<NAME>`
 * or `ApiRoutes.<name>(<arguments>)`. Any other `ApiRoutes.` use outside comments (a route kept in a variable,
 * another client's name, a name [ApiRoutes] does not have) is unread: its method cannot be told, so the check
 * fails on it rather than skip it.
 */
internal fun clientCalls(source: String): ClientScan {
    val code = withoutComments(source)
    val calls = mutableListOf<Pair<String, String>>()
    val read = mutableSetOf<Int>()
    CLIENT_CALL.findAll(code).forEach { match ->
        val route = match.groups[2]!!
        val path = routePath(match.groupValues[3], arity(code, match.range.last + 1))
        if (path != null) {
            calls += match.groupValues[1] to path
            read += route.range.first
        }
    }
    val unread = ROUTES_USE.findAll(code)
        .map { it.range.first }
        .filterNot { it in read }
        .map { lineAt(source, it) }
        .distinct()
        .toList()
    return ClientScan(calls, unread)
}

private val CLIENT_CALL = Regex("""\bclient\.(get|post|put|patch|delete)\(\s*(ApiRoutes)\.(\w+)""")
private val ROUTES_USE = Regex("""\bApiRoutes\.""")

/** Path parameters, whatever their names, compare equal. */
private fun normalized(path: String) = path.replace(Regex("\\{[^}]*\\}"), "{}")

/** [ApiRoutes]' string constants by name. */
private fun routeConstants(): Map<String, String> = ApiRoutes::class.java.declaredFields
    .filter { Modifier.isStatic(it.modifiers) && it.type == String::class.java }
    .associate { it.name to it.get(null) as String }

/** [ApiRoutes]' public functions that build a path from string arguments. */
private fun routeBuilders(): List<Method> = ApiRoutes::class.java.declaredMethods
    .filter { Modifier.isPublic(it.modifiers) && !Modifier.isStatic(it.modifiers) }
    .filter { it.returnType == String::class.java && it.parameterTypes.all { p -> p == String::class.java } }

/** [method] called with a path parameter for each argument. */
private fun buildRoute(method: Method): String =
    method.invoke(ApiRoutes, *Array(method.parameterCount) { "{}" }) as String

/** The normalized path of `ApiRoutes.<name>`: a constant when [arity] is null, else a builder of [arity] arguments. */
private fun routePath(name: String, arity: Int?): String? {
    val route = if (arity == null) {
        routeConstants()[name]
    } else {
        routeBuilders().firstOrNull { it.name == name && it.parameterCount == arity }?.let(::buildRoute)
    }
    return route?.let { normalized("/$it") }
}

/**
 * The number of arguments in the argument list that opens at [from] (after whitespace), -1 when it does not
 * close; null when no argument list follows.
 */
private fun arity(code: String, from: Int): Int? {
    var index = from
    while (index < code.length && code[index].isWhitespace()) index++
    if (index >= code.length || code[index] != '(') return null
    var depth = 0
    var commas = 0
    var blank = true
    for (i in index until code.length) {
        when (code[i]) {
            '(' -> depth++
            ')' -> if (--depth == 0) return if (blank) 0 else commas + 1
            ',' -> if (depth == 1) commas++
        }
        if (depth >= 1 && i > index && !code[i].isWhitespace()) blank = false
    }
    return -1
}

/** [source] with its comments blanked out, line breaks kept so offsets and lines stay; strings are left as they are. */
private fun withoutComments(source: String): String {
    val code = StringBuilder(source)
    var i = 0
    var quote: String? = null
    while (i < source.length) {
        val open = quote
        when {
            open != null -> when {
                open == "\"" && source[i] == '\\' -> i++
                source.startsWith(open, i) -> {
                    i += open.length - 1
                    quote = null
                }
            }
            source.startsWith("\"\"\"", i) -> {
                quote = "\"\"\""
                i += 2
            }
            source[i] == '"' -> quote = "\""
            source[i] == '\'' -> i = source.indexOf('\'', if (source.getOrNull(i + 1) == '\\') i + 3 else i + 2)
                .let { if (it < 0) source.length else it }
            source.startsWith("//", i) -> i = blank(code, i, source.indexOf('\n', i)) - 1
            source.startsWith("/*", i) -> {
                val close = source.indexOf("*/", i + 2)
                i = blank(code, i, if (close < 0) -1 else close + 2) - 1
            }
        }
        i++
    }
    return code.toString()
}

/** Blanks [code] from [start] to [end] (the end of the text when -1) but its line breaks; returns the end. */
private fun blank(code: StringBuilder, start: Int, end: Int): Int {
    val stop = if (end < 0) code.length else end
    for (i in start until stop) if (code[i] != '\n') code[i] = ' '
    return stop
}

/** The trimmed line of [source] that holds [index]. */
private fun lineAt(source: String, index: Int): String {
    val start = source.lastIndexOf('\n', index - 1) + 1
    val end = source.indexOf('\n', index).let { if (it < 0) source.length else it }
    return source.substring(start, end).trim()
}
