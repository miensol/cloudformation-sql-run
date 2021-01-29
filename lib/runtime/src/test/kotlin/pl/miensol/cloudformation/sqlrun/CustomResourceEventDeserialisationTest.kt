package pl.miensol.cloudformation.sqlrun

import kotlinx.serialization.json.Json
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.Matchers
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import pl.miensol.shouldko.shouldBe
import pl.miensol.shouldko.shouldEqual
import pl.miensol.shouldko.shouldNotBe

internal class CustomResourceEventDeserialisationTest {
    val json = """{
    "RequestType": "Create",
    "ServiceToken": "arn:aws:lambda:eu-west-1:XXXXXXXXXXX:function:SqlRunExample-CreateDatabaseUserProviderSqlRunfram-1C34PDCUEVRR4",
    "ResponseURL": "https://cloudformation-custom-resource-response-euwest1.s3-eu-west-1.amazonaws.com/arn%3Aaws%3Acloudformation%3Aeu-west-1%3AXXXXXXXXXXX%3Astack/SqlRunExample/XXXXXXXXXXX%7CCreateDatabaseUser582E230D%7C122848b4-eccc-4146-8826-94db7e940bb2?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Date=20210125T060251Z&X-Amz-SignedHeaders=host&X-Amz-Expires=7199&X-Amz-Credential=AKIAJ7MCS7PVEUOADEEA%2F20210125%2Feu-west-1%2Fs3%2Faws4_request&X-Amz-Signature=8f274b9292c698e782529d3faf6cc8859bc1e8ab476f0c725025dcbfb3256ab5",
    "StackId": "arn:aws:cloudformation:eu-west-1:XXXXXXXXXXX:stack/SqlRunExample/XXXXXXXXXXX",
    "RequestId": "122848b4-eccc-4146-8826-94db7e940bb2",
    "LogicalResourceId": "CreateDatabaseUser582E230D",
    "ResourceType": "Custom::SqlRun",
    "ResourceProperties": {
        "ServiceToken": "arn:aws:lambda:eu-west-1:XXXXXXXXXXX:function:SqlRunExample-CreateDatabaseUserProviderSqlRunfram-1C34PDCUEVRR4",
        "connection": {
            "password": "{{resolve:secretsmanager:arn:aws:secretsmanager:eu-west-1:XXXXXXXXXXX:secret:developmentDatabaseSecretC0-RHxIoHua2RnU:SecretString:password::}}",
            "database": "relevo",
            "port": "3306",
            "host": "development-backend.cbjzuknka2om.eu-west-1.rds.amazonaws.com",
            "type": "driverTypeHostPort",
            "driverType": "mysql",
            "username": "relevo_admin"
        },
        "up": {
            "run": [
                {
                    "parameters": {
                        "password": "{{resolve:secretsmanager:arn:aws:secretsmanager:eu-west-1:XXXXXXXXXXX:secret:SomePasswordXXXXXXXXXXX:SecretString:::}}"
                    },
                    "sql": "CREATE USER 'myDatabaseUser'@'%' IDENTIFIED BY ':password';\n          \nGRANT ALL ON relevo.* TO 'myDatabaseUser'@'%';\n\nFLUSH privileges\n"
                }
            ]
        },
        "down": {
            "run": [
                {
                    "sql": "DROP USER 'myDatabaseUser'@'%'"
                }
            ]
        }
    }
}"""

    @Test
    fun `can deserialise`() {
        //given
        val decoded = Json.decodeFromString(CustomResourceEvent.serializer(), json)

        //then
        decoded.shouldBe(Matchers.instanceOf(CustomResourceEvent.Create::class.java))

        decoded.resourceProperties.up.run.size.shouldEqual(1)
        decoded.resourceProperties.up.run[0].parameters?.get("password").shouldNotBe(nullValue())
        decoded.resourceProperties.down?.run?.size.shouldEqual(1)
    }
}