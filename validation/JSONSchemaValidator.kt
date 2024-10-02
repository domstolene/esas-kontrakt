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
    private val schema = loadSchema(schemaUri.toString())

    private fun loadSchema(schemaPath: String): JsonSchemaString {
        val schemaStream: InputStream = this::class.java.classLoader.getResourceAsStream(schemaPath)
            ?: throw JSONSchemaException("Something went wrong while reading from $schemaPath")

        schemaStream.bufferedReader(Charsets.UTF_8).use {
            val jsonSchemaAsString = it.readText()
            if (jsonSchemaAsString.isBlank()) {
                throw JSONSchemaException("Loaded schema is empty at $schemaPath")
            }

            return JsonSchemaString(jsonSchemaAsString)
        }
    }

    fun validateOrThrow(data: String): String {
        val jsonSchema: JSONSchema = JSONSchema.parse(schema.value)
        val validationResult = jsonSchema.validateBasic(data)

        validationResult.takeIf { res -> !res.valid }?.let {
            val errors = it.errors?.map { err -> SchemaValidationError(err.error, err.instanceLocation) }
                ?.distinctError()
            throw FailedValidationException("Validation failed: $errors")
        }
        return data
    }

    /**
     * Processes a list of `SchemaValidationError` objects and returns a single formatted error message.
     * The method groups errors by their location, filters out less specific ones, and ensures the uniqueness of
     * error messages within the most specific location.
     *
     * @return A formatted string containing the most nested error messages occurred during validation.
     */
    private fun List<SchemaValidationError>.distinctError(): String {
        val groupedErrors = this.groupBy { it.location }
        val sorted = groupedErrors.entries.sortedByDescending { it.key.length }

        val processedLocations = mutableSetOf<String>()
        val uniqueErrors = sorted.flatMap { (location, errors) ->
            if (processedLocations.none { location.startsWith(it) }) {
                processedLocations.add(location)
                errors.distinctBy { it.error }
            } else emptyList()
        }

        // Format the errors
        return uniqueErrors.groupBy { it.location }.map { (location, errs) ->
            "error found at $location:\n" + errs.joinToString("\n") { "  - ${it.error}" }
        }.joinToString("\n")
    }
}

class FailedValidationException(error: String) : Exception(error)