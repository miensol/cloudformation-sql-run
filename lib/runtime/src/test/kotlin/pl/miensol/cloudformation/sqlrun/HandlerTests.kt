package pl.miensol.cloudformation.sqlrun

import com.amazonaws.services.lambda.runtime.LambdaRuntime
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.Test
import org.testcontainers.containers.JdbcDatabaseContainer
import org.testcontainers.containers.MySQLContainerProvider
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import pl.miensol.shouldko.shouldEqual
import java.sql.ResultSet

@Testcontainers
internal class HandlerMysqlTestTests {
    val sut = Handler()

    @Container
    val mysql = MySQLContainerProvider().newInstance()
        .withUsername("root")
        .withPassword("")

    val connection
        get() = newMysqlDriverTypeHostPort()
            .copy(
                username = mysql.username,
                password = mysql.password,
                host = "localhost",
                port = mysql.firstMappedPort,
                database = "mysql"
            )

    val event
        get() = newCreateEvent(
            CfnSqlRuns(
                listOf(
                    CfnSqlStatement(
                        "CREATE USER 'myDatabaseUser'@'%' IDENTIFIED BY :password",
                        mapOf("password" to JsonPrimitive("secret"))
                    )
                )
            ),
            connection
        )

    @Test
    fun `can execute in mysql`() {
        //when
        sut.handleRequest(event, LambdaRuntime.getLogger())

        //then
        val matchingUser =
            mysql.executeQuery("select user from mysql.user where user = 'myDatabaseUser'") {
                mapOf(
                    "user" to getString("user"),
                )
            }

        matchingUser.size.shouldEqual(1)
        matchingUser[0]["user"].shouldEqual("myDatabaseUser")
    }


}

fun <T> JdbcDatabaseContainer<out JdbcDatabaseContainer<*>>.executeQuery(
    query: String,
    buildRow: ResultSet.() -> T
): List<T> {
    return createConnection("").use {
        val resultSet = it.prepareStatement(query).executeQuery()
        resultSet.use {
            sequence {
                while (it.next()) {
                    yield(buildRow(it))
                }
            }.toList()
        }
    }
}

private fun newCreateEvent(cfnSqlRuns: CfnSqlRuns, connection: CfnSqlRunConnection) = CustomResourceEvent.Create(
    serviceToken = "test.serviceToken",
    responseURL = "test.responseURL",
    stackId = "test.stackId",
    requestId = "test.requestId",
    logicalResourceId = "test.logicalResourceId",
    resourceType = "test.resourceType",
    resourceProperties = CfnSqlRunProps(
        ServiceToken = "test.serviceToken",
        connection = connection,
        up = cfnSqlRuns
    ),
    requestType = "Create"
)

fun newMysqlDriverTypeHostPort() = CfnSqlRunConnection.DriverTypeHostPort(
    driverType = ConnectionDriverType.mysql,
    username = "test-username",
    password = "test-password",
    database = "test-database",
    host = "database",
    port = 3306
)
