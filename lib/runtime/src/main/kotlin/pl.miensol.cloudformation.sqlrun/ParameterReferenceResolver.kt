package pl.miensol.cloudformation.sqlrun

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest
import software.amazon.awssdk.services.ssm.SsmClient
import software.amazon.awssdk.services.ssm.model.GetParameterRequest

internal interface ParameterReferenceResolver {
    fun resolve(value: String): String
}

internal fun awsDynamicReferencesResolver() = CompositeParameterReferenceResolver(
    listOf(SecretManagerParameterReferenceResolver(), SystemsManagerParameterReferenceResolver())
)

internal class CompositeParameterReferenceResolver(private val resolvers: List<ParameterReferenceResolver>) :
    ParameterReferenceResolver {
    override fun resolve(value: String): String {
        return resolvers.fold(value) { v, resolver -> resolver.resolve(v) }
    }
}

internal class SystemsManagerParameterReferenceResolver(
    private val ssm: SsmClient = SsmClient.create()
) : ParameterReferenceResolver {
    private val matcher = Regex(
        """\{\{resolve:ssm(-secure)?:(?<name>[a-zA-Z0-9_.\-/]+):(?<version>\d+)\}\}"""
    )

    override fun resolve(value: String): String {
        return matcher.replace(value) { matchResult ->
            val name = matchResult.groups["name"]?.value

            val version = matchResult.groups["version"]?.value

            val nameWithVersion = if (version.isNullOrBlank()) name else "${name}:${version}"

            val value = ssm.getParameter(
                GetParameterRequest.builder().name(nameWithVersion).withDecryption(true).build()
            )

            value.parameter().value()
        }
    }

}

internal class SecretManagerParameterReferenceResolver(
    private val secretManager: SecretsManagerClient = SecretsManagerClient.create()
) : ParameterReferenceResolver {
    private val matcher = Regex(
        "\\{\\{resolve:secretsmanager:(?<arn>[^}]+secret:[^:]+):SecretString:(?<jsonKey>[^:]*):(?<versionStage>[^:]*):(?<versionId>[^:}]*)}}",
        RegexOption.IGNORE_CASE
    )

    override fun resolve(value: String): String {
        return matcher.replace(value) { matchResult ->
            val arn = matchResult.groups["arn"]?.value
            val jsonKey = matchResult.groups["jsonKey"]?.value?.ifEmpty { null }
            val versionStage = matchResult.groups["versionStage"]?.value?.ifEmpty { null }
            val versionId = matchResult.groups["versionId"]?.value?.ifEmpty { null }

            val secretValueResponse = secretManager.getSecretValue(
                GetSecretValueRequest.builder().secretId(arn).versionId(versionId).versionStage(versionStage).build()
            )

            val secretValue = secretValueResponse.secretString()

            if (jsonKey.isNullOrBlank()) {
                secretValue
            } else {
                val secretJson = Json.decodeFromString(JsonObject.serializer(), secretValue)
                val jsonElement = secretJson[jsonKey]!!
                if (jsonElement is JsonPrimitive && jsonElement.isString) {
                    jsonElement.content
                } else {
                    jsonElement.toString()
                }
            }
        }
    }
}