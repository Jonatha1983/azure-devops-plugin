// PipelineParameter.kt
package com.dorkag.azure_devops.extensions.pipeline

import com.dorkag.azure_devops.dto.Parameter
import com.dorkag.azure_devops.dto.ParameterType
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import javax.inject.Inject

abstract class PipelineParameter @Inject constructor(objects: ObjectFactory) {
    abstract val displayName: Property<String>
    abstract val type: Property<String>        // e.g. "string", "boolean", etc.
    abstract val default: Property<String>
    private val values: ListProperty<String> = objects.listProperty(String::class.java)

    // Expose `values` via a protected getter
    internal fun tValues(): ListProperty<String> = values

    // Gradle NamedDomainObjectContainer will set this after creation
    lateinit var name: String

    fun toDto(): Parameter {
        // If the user sets type="string", we keep it lowercase in YAML
        // but still map to the correct ParameterType enum.
        val typeString = this.type.orNull ?: "string"   // fallback
        val enumVal = runCatching { ParameterType.valueOf(typeString.uppercase()) }
            .getOrDefault(ParameterType.STRING)

        return Parameter(
            name = this.name,
            displayName = this.displayName.orNull ?: this.name,
            type = enumVal,          // The enum internally is STRING/BOOLEAN/etc.
            default = this.default.orNull,
            values = this.values.orNull
        )
    }
}