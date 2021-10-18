package pl.miensol.cloudformation.sqlrun

import com.amazonaws.services.lambda.runtime.LambdaRuntime
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.Test
import org.testcontainers.containers.JdbcDatabaseContainer
import org.testcontainers.containers.MySQLContainerProvider
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import pl.miensol.shouldko.shouldContain
import pl.miensol.shouldko.shouldEqual
import java.sql.ResultSet

@Testcontainers
internal class HandlerMysqlTestTests {
    val resolver = mockk<ParameterReferenceResolver> {
        every {
            resolve(any())
        } answers {
            arg(0)
        }
    }

    val sut = Handler(
        resolver = resolver
    )

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
                database = "mysql",
                options = mapOf(
                    "useSSL" to "false",
                    "trustServerCertificate" to "true"
                )
            )

    private val logger = LambdaRuntime.getLogger()

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
        sut.handleRequest(
            event,
            logger
        )

        //then
        val matchingUser =
            mysql.executeQuery("select user from mysql.user where user = 'myDatabaseUser'") {
                mapOf(
                    "user" to getString("user"),
                )
            }

        matchingUser.size.shouldEqual(1)
        matchingUser[0]["user"].shouldEqual("myDatabaseUser")
        verify { resolver.resolve("secret") }
    }

    @Test
    fun `can execute multiple statements`() {
        //given
        mysql.executeChanges(
            "use test",
            "create table items (id varchar(100) primary key)"
        )

        val event = newCreateEvent(
            CfnSqlRuns(
                listOf(
                    CfnSqlStatement(
                        "insert into items values ('one')"
                    ),
                    CfnSqlStatement(
                        "insert into items values ('two')"
                    )
                )
            ),
            connection = connection.copy(database = "test")
        )

        //when
        sut.handleRequest(
            event,
            logger
        )

        //then
        val items =
            mysql.executeQuery("select id from items") {
                getString("id")
            }

        items.size.shouldEqual(2)
        items.shouldContain("one")
        items.shouldContain("two")
    }

    @Test
    fun `executes multiple statements in transaction`() {
        //given
        mysql.executeChanges(
            "use test",
            "create table items (id varchar(100) primary key)"
        )

        val event = newCreateEvent(
            CfnSqlRuns(
                listOf(
                    CfnSqlStatement(
                        "insert into items values ('one')"
                    ),
                    CfnSqlStatement(
                        "insert into items values ('one')"
                    )
                )
            ),
            connection = connection.copy(database = "test")
        )

        //when
        val result = runCatching {
            sut.handleRequest(
                event,
                logger
            )
        }

        //then
        result.isFailure.shouldEqual(true)
        val items = mysql.executeQuery("select id from items") {
            getString("id")
        }

        items.size.shouldEqual(0)
    }

    @Test
    fun `can select values`() {
        //given
        mysql.executeChanges(
            "use test",
            "create table items (id varchar(100) primary key, name text, price int)",
            "insert into items values ('one', 'One', 10)",
            "insert into items values ('two', 'Two', 20)"
        )

        val event = newCreateEvent(
            CfnSqlRuns(
                listOf(
                    CfnSqlStatement(
                        "select id as Id, name as Name, price as Price from items"
                    )
                )
            ),
            connection = connection.copy(database = "test")
        )

        //when
        val result = runCatching {
            sut.handleRequest(
                event,
                logger
            )
        }

        //then
        result.isFailure.shouldEqual(false)
        val data = result.getOrThrow().Data as JsonObject
        data["0.0.Id"].shouldEqual(JsonPrimitive("one"))
        data["0.0.Name"].shouldEqual(JsonPrimitive("One"))
        data["0.0.Price"].shouldEqual(JsonPrimitive(10))
    }

    @Test
    fun `delete event executes down`() {
        //given
        mysql.executeChanges(
            "use test",
            "create table items (id varchar(100) primary key)"
        )

        val create = newCreateEvent(
            CfnSqlRuns(
                listOf(
                    CfnSqlStatement(
                        "insert into items values ('one')"
                    )
                )
            ),
            down = CfnSqlRuns(
                listOf(
                    CfnSqlStatement(
                        "delete from items where id = 'one'"
                    )
                )
            ),
            connection = connection.copy(database = "test")
        )
        sut.handleRequest(create, logger)


        //when
        val delete = newDeleteEvent(
            up = create.resourceProperties.up,
            down = CfnSqlRuns(
                listOf(
                    CfnSqlStatement(
                        "delete from items where id = 'one'"
                    )
                )
            ),
            connection.copy(database = "test")
        )
        sut.handleRequest(delete, logger)

        //then
        val items = mysql.executeQuery("select id from items") {
            getString("id")
        }

        items.size.shouldEqual(0)
    }

    @Test
    fun `update event executes previous down`() {
        //given
        mysql.executeChanges(
            "use test",
            "create table items (id varchar(100) primary key)"
        )

        val create = newCreateEvent(
            CfnSqlRuns(
                listOf(
                    CfnSqlStatement(
                        "insert into items values ('one')"
                    )
                )
            ),
            down = CfnSqlRuns(
                listOf(
                    CfnSqlStatement(
                        "delete from items where id = 'one'"
                    )
                )
            ),
            connection = connection.copy(database = "test")
        )
        sut.handleRequest(create, logger)


        //when
        val update = newUpdateEvent(
            up = CfnSqlRuns(
                listOf(
                    CfnSqlStatement(
                        "insert into items values ('updated')"
                    )
                )
            ),
            previous = create,
            connection = connection.copy(database = "test"),
        )
        sut.handleRequest(update, logger)

        //then
        val items = mysql.executeQuery("select id from items") {
            getString("id")
        }

        items.shouldEqual(listOf("updated"))
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

fun JdbcDatabaseContainer<out JdbcDatabaseContainer<*>>.executeChange(query: String): Int {
    return createConnection("").use {
        it.prepareStatement(query).executeUpdate()
    }
}

fun JdbcDatabaseContainer<out JdbcDatabaseContainer<*>>.executeChanges(vararg queries: String): List<Int> {
    return queries.map { executeChange(it) }
}

private fun newCreateEvent(
    up: CfnSqlRuns,
    down: CfnSqlRuns = CfnSqlRuns(emptyList()),
    connection: CfnSqlRunConnection
) = CustomResourceEvent.Create(
    serviceToken = "test.serviceToken",
    responseURL = "test.responseURL",
    stackId = "test.stackId",
    requestId = "test.requestId",
    logicalResourceId = "test.logicalResourceId",
    resourceType = "test.resourceType",
    resourceProperties = CfnSqlRunProps(
        ServiceToken = "test.serviceToken",
        connection = connection,
        up = up,
        down = down
    ),
    requestType = "Create"
)

private fun newDeleteEvent(
    up: CfnSqlRuns = CfnSqlRuns(emptyList()),
    down: CfnSqlRuns,
    connection: CfnSqlRunConnection
) = CustomResourceEvent.Delete(
    serviceToken = "test.serviceToken",
    responseURL = "test.responseURL",
    stackId = "test.stackId",
    requestId = "test.requestId",
    logicalResourceId = "test.logicalResourceId",
    resourceType = "test.resourceType",
    physicalResourceId = "test.physicalResourceId",
    resourceProperties = CfnSqlRunProps(
        ServiceToken = "test.serviceToken",
        connection = connection,
        up = up,
        down = down,
    ),
    requestType = "Delete"
)

private fun newUpdateEvent(
    up: CfnSqlRuns = CfnSqlRuns(emptyList()),
    down: CfnSqlRuns = CfnSqlRuns(emptyList()),
    previous: CustomResourceEvent,
    connection: CfnSqlRunConnection
) = CustomResourceEvent.Update(
    serviceToken = "test.serviceToken",
    responseURL = "test.responseURL",
    stackId = "test.stackId",
    requestId = "test.requestId",
    logicalResourceId = "test.logicalResourceId",
    resourceType = "test.resourceType",
    physicalResourceId = "test.physicalResourceId",
    resourceProperties = CfnSqlRunProps(
        ServiceToken = "test.serviceToken",
        connection = connection,
        up = up,
        down = down,
    ),
    oldResourceProperties = previous.resourceProperties,
    requestType = "Update"
)

fun newMysqlDriverTypeHostPort() = CfnSqlRunConnection.DriverTypeHostPort(
    driverType = ConnectionDriverType.mysql,
    username = "test-username",
    password = "test-password",
    database = "test-database",
    host = "database",
    port = 3306
)
