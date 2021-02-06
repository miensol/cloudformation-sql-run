package pl.miensol.cloudformation.sqlrun

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.Test
import pl.miensol.shouldko.shouldEqual
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient
import software.amazon.awssdk.services.secretsmanager.model.CreateSecretResponse
import java.util.*

internal class SecretManagerParameterReferenceResolverTests {
    val client = SecretsManagerClient.create()

    val sut = SecretManagerParameterReferenceResolver(client)

    @Test
    fun `can resolve plain secret reference`() {
        //given
        val secretName = "secret${Random().nextInt()}"

        client.createSecret {
            it
                .name(secretName)
                .secretString("secret value")
        }.use { secretResponse ->

            val reference =
                "{{resolve:secretsmanager:${secretResponse.arn()}:SecretString:::${secretResponse.versionId()}}}"

            //when
            val resolved = sut.resolve(reference)

            //then
            resolved.shouldEqual("secret value")
        }
    }

    @Test
    fun `can resolve json plain secret reference`() {
        //given
        val secretName = "secret${Random().nextInt()}"
        client.createSecret {
            it
                .name(secretName)
                .secretString(
                    Json.encodeToString(
                        JsonObject.serializer(),
                        JsonObject(mapOf("jsonValue" to JsonPrimitive("secret value")))
                    )
                )
        }.use { secretResponse ->
            val reference =
                "{{resolve:secretsmanager:${secretResponse.arn()}:SecretString:jsonValue::${secretResponse.versionId()}}}"

            //when
            val resolved = sut.resolve(reference)

            //then
            resolved.shouldEqual("secret value")
        }
    }

    private fun CreateSecretResponse.use(block: (CreateSecretResponse) -> Any?) {
        try {
            block(this)
        } finally {
            client.deleteSecret {
                it.secretId(arn())
            }
        }
    }
}

