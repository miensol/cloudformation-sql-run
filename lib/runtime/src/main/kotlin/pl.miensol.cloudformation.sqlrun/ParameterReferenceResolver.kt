package pl.miensol.cloudformation.sqlrun

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest

internal interface ParameterReferenceResolver {
    fun resolve(value: String): String
}

internal fun awsDynamicReferencesResolver() = CompositeParameterReferenceResolver(
    listOf(SecretManagerParameterReferenceResolver())
)

internal class CompositeParameterReferenceResolver(private val resolvers: List<ParameterReferenceResolver>) :
    ParameterReferenceResolver {
    override fun resolve(value: String): String {
        return resolvers.fold(value) { v, resolver -> resolver.resolve(v) }
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
            val jsonKey = matchResult.groups["jsonKey"]?.value
            val versionStage = matchResult.groups["versionStage"]?.value
            val versionId = matchResult.groups["versionId"]?.value

            val secretValueResponse = secretManager.getSecretValue(
                GetSecretValueRequest.builder()
                    .secretId(arn)
                    .versionId(versionId)
                    .versionStage(versionStage)
                    .build()
            )

            val secretValue = secretValueResponse.secretString()

            if (jsonKey.isNullOrBlank()) {
                secretValue
            } else {
                val secretJson = Json.decodeFromString(JsonObject.serializer(), secretValue)
                val jsonElement = secretJson[jsonKey]!!
                jsonElement.toString()
            }
        }
    }
}