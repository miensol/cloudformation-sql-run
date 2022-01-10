package pl.miensol.cloudformation.sqlrun

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.Test
import org.testcontainers.containers.MySQLContainerProvider
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import pl.miensol.shouldko.shouldEqual

@Testcontainers
internal class HandlerMysqlTestTests : ItemsTableSqlTests {
    val resolver = mockk<ParameterReferenceResolver> {
        every {
            resolve(any())
        } answers {
            arg(0)
        }
    }

    override val handler = Handler(
        resolver = resolver
    )

    companion object {
        @Container
        @JvmStatic
        val mysql = MySQLContainerProvider().newInstance()
            .withUsername("root")
            .withPassword("")
            .withReuse(true)
            .withDatabaseName("mysql")
    }

    override val database = mysql

    override val connection
        get() = newMysqlDriverTypeHostPort()
            .copy(
                username = database.username,
                password = database.password,
                host = "localhost",
                port = database.firstMappedPort,
                database = database.getDatabaseName(),
                options = mapOf(
                    "useSSL" to "false",
                    "trustServerCertificate" to "true"
                )
            )

    @Test
    fun `can execute in mysql`() {
        //given
        val event = newCreateEvent(
            up = CfnSqlRuns(
                listOf(
                    CfnSqlStatement(
                        "CREATE USER 'myDatabaseUser'@'%' IDENTIFIED BY :password",
                        mapOf("password" to JsonPrimitive("secret"))
                    )
                )
            ),
            connection = connection
        )

        //when
        handler.handleRequest(
            event,
            logger
        )

        //then
        val matchingUser =
            database.executeQuery("select user from mysql.user where user = 'myDatabaseUser'") {
                mapOf(
                    "user" to getString("user"),
                )
            }

        matchingUser.size.shouldEqual(1)
        matchingUser[0]["user"].shouldEqual("myDatabaseUser")
        verify { resolver.resolve("secret") }
    }


}