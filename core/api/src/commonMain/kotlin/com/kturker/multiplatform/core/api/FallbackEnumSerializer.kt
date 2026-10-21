package com.kturker.multiplatform.core.api

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * A response enum that a newer server may extend: a value the app does not know decodes to [fallback] instead
 * of failing the whole response. The [fallback] is never sent, so encoding it throws [SerializationException].
 *
 * The [descriptor] is an enum of the server's values only ([wireName] of every entry but [fallback], in
 * declaration order), so ApiContractTest compares it with the API description as it would a plain enum.
 */
open class FallbackEnumSerializer<E : Enum<E>>(
    serialName: String,
    entries: List<E>,
    private val fallback: E,
    private val wireName: (E) -> String
) : KSerializer<E> {

    private val known = entries - fallback
    private val byWireName = known.associateBy(wireName)

    // Built like the plugin's own enum descriptor: kind ENUM, one OBJECT element per value
    @OptIn(InternalSerializationApi::class)
    override val descriptor: SerialDescriptor = buildSerialDescriptor(serialName, SerialKind.ENUM) {
        known.forEach { entry ->
            val name = wireName(entry)
            element(name, buildSerialDescriptor("$serialName.$name", StructureKind.OBJECT))
        }
    }

    override fun serialize(
        encoder: Encoder,
        value: E
    ) {
        if (value == fallback) {
            val name = "${descriptor.serialName}.${value.name}"
            throw SerializationException("$name stands for a value the app does not know and is never sent")
        }
        encoder.encodeString(wireName(value))
    }

    override fun deserialize(decoder: Decoder): E = byWireName[decoder.decodeString()] ?: fallback
}
