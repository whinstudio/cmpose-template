package com.kturker.multiplatform.core.api

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assume
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Checks the client's routes and models against the backend's API description (OpenAPI 3), which the
 * server's `writeOpenApi` task writes to `server/build/openapi/openapi.json`; the build hands its path over
 * as the `cmpose.openapi` system property. Skipped when there is no such file (no server written yet),
 * unless the build says the server is there (`cmpose.openapi.required=true`): then a missing file fails.
 */
@OptIn(ExperimentalSerializationApi::class)
class ApiContractTest {

    private val document: JsonObject by lazy { loadDocument() }

    @Test
    fun everyClientRouteExistsWithItsMethod() {
        val paths = document.getValue("paths").jsonObject.mapKeys { (path, _) -> normalized(path) }
        val problems = CLIENT_ROUTES.mapNotNull { (method, route) ->
            val path = normalized("/$route")
            val operations = paths[path]?.jsonObject
            when {
                operations == null -> "$path: not in the API description"
                method !in operations -> "$path: no ${method.uppercase()} (has ${operations.keys - "parameters"})"
                else -> null
            }
        }
        assertTrue(problems.isEmpty(), "Client routes the server does not serve:\n" + problems.joinToString("\n"))
    }

    @Test
    fun everyModelMatchesItsSchema() {
        val problems = mutableListOf<String>()
        MODELS.forEach { model ->
            val schema = schemas[model.schema]?.jsonObject
            if (schema == null) {
                problems += "${model.schema}: not in the API description"
            } else {
                check(model.schema, model.descriptor, schema, model.response, problems)
            }
        }
        assertTrue(problems.isEmpty(), "Models that do not match the server:\n" + problems.joinToString("\n"))
    }

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
        val name = ref.removePrefix("#/components/schemas/")
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

    private class Model(val schema: String, val descriptor: SerialDescriptor, val response: Boolean)

    private companion object {
        const val OPENAPI_PROPERTY = "cmpose.openapi"
        const val REQUIRED_PROPERTY = "cmpose.openapi.required"
        const val ID = "{id}"
        const val USER_ID = "{userId}"
        const val INVITE_ID = "{inviteId}"

        /** Path parameters, whatever their names, compare equal. */
        fun normalized(path: String) = path.replace(Regex("\\{[^}]*\\}"), "{}")

        val CLIENT_ROUTES = listOf(
            "get" to ApiRoutes.ME,
            "delete" to ApiRoutes.ME,
            "put" to ApiRoutes.PROFILE,
            "get" to ApiRoutes.SPACES,
            "post" to ApiRoutes.SPACES,
            "patch" to ApiRoutes.space(ID),
            "delete" to ApiRoutes.space(ID),
            "get" to ApiRoutes.members(ID),
            "patch" to ApiRoutes.member(ID, USER_ID),
            "delete" to ApiRoutes.member(ID, USER_ID),
            "post" to ApiRoutes.leave(ID),
            "get" to ApiRoutes.invites(ID),
            "post" to ApiRoutes.invites(ID),
            "delete" to ApiRoutes.invite(ID, INVITE_ID),
            "post" to ApiRoutes.INVITE_PREVIEW,
            "post" to ApiRoutes.INVITE_ACCEPT,
            "post" to ApiRoutes.FILES,
            "get" to ApiRoutes.file(ID),
            "delete" to ApiRoutes.file(ID),
        )

        /** Each model with the schema it maps to; `response` for what the server sends. */
        val MODELS = listOf(
            Model("Role", Role.serializer().descriptor, response = true),
            Model("InviteState", InviteState.serializer().descriptor, response = true),
            Model("Space", Space.serializer().descriptor, response = true),
            Model("SpaceDraft", SpaceDraft.serializer().descriptor, response = false),
            Model("Member", Member.serializer().descriptor, response = true),
            Model("RoleChange", RoleChange.serializer().descriptor, response = false),
            Model("InviteRequest", InviteRequest.serializer().descriptor, response = false),
            Model("Invite", InviteLink.serializer().descriptor, response = true),
            Model("PendingInvite", PendingInvite.serializer().descriptor, response = true),
            Model("InviteToken", InviteToken.serializer().descriptor, response = false),
            Model("InvitePreview", InvitePreview.serializer().descriptor, response = true),
            Model("Me", Me.serializer().descriptor, response = true),
            Model("ProfileUpdate", Profile.serializer().descriptor, response = false),
            Model("File", FileInfo.serializer().descriptor, response = true),
            Model("Error", ErrorBody.serializer().descriptor, response = true),
        )
    }
}
