import java.lang.Class
import net.pwall.json.schema.JSONSchema
import net.pwall.json.schema.JSONSchemaException
import java.io.InputStream
import java.net.URI

/**
 * Represents a validation error that occurred during schema validation.
 *
 * @property error The error message describing the validation error.
 * @property location The location where the validation error occurred.
 */
data class SchemaValidationError(
    val error: String,
    val location: String,
) {
    override fun toString(): String {
        return "\"$error\" in location \"$location\""
    }
}

@JvmInline
value class JsonSchemaString(val value: String)

/**
 * A utility class for validating JSON data against a JSON schema.
 *
 * @param clazz The class for which the JSON schema is defined.
 */
class JSONSchemaValidator(schemaUri: URI) {
    private val schemaCache = HashMap<URI, JsonSchemaString>()
    private var schema: JsonSchemaString = schemaCache.getOrPut(schemaUri) {
        val schemaStream: InputStream = this::class.java.classLoader.getResourceAsStream(schemaUri.toString())
            ?: throw JSONSchemaException("Something went wrong while reading from $schemaUri")

        schemaStream.use {
            val jsonSchemaAsString = it.bufferedReader(Charsets.UTF_8).readText()
            JsonSchemaString(jsonSchemaAsString)
        }
    }

    fun validateOrThrow(data: String): String {
        val jsonSchema: JSONSchema = JSONSchema.parse(schema.value)
        val errorMessage = jsonSchema.validateBasic(data)

        errorMessage.takeIf { error -> !error.valid }?.let {
            val errors = it.errors?.map { err -> SchemaValidationError(err.error, err.instanceLocation) }
            throw FailedValidationException("Validation failed: $errors}")
        }
        return data
    }
}

class FailedValidationException(error: String) : Exception(error)