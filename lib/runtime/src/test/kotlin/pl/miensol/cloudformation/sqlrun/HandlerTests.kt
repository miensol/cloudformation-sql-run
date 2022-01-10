package pl.miensol.cloudformation.sqlrun

import com.amazonaws.services.lambda.runtime.LambdaRuntime
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.testcontainers.containers.JdbcDatabaseContainer
import pl.miensol.shouldko.shouldContain
import pl.miensol.shouldko.shouldEqual
import java.sql.ResultSet

internal interface HasHandler {
    val handler: Handler
    val logger get() = LambdaRuntime.getLogger()
}

interface HasDatabase {
    val database: JdbcDatabaseContainer<*>
    val connection: CfnSqlRunConnection
}

internal interface ItemsTableSqlTests : HasDatabase, HasHandler {
    @Test
    fun `can execute multiple statements`() {
        //given
        database.executeChanges(

            "drop table if exists items",
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
            connection = connection
        )

        //when
        handler.handleRequest(
            event,
            logger
        )

        //then
        val items =
            database.executeQuery("select id from items") {
                getString("id")
            }

        items.size.shouldEqual(2)
        items.shouldContain("one")
        items.shouldContain("two")
    }

    @Test
    fun `can execute with multiple parameters`() {
        //given
        database.executeChanges(
            "drop table if exists items",
            "create table items (name text, price int)"
        )

        val event = newCreateEvent(
            up = CfnSqlRuns(
                listOf(
                    CfnSqlStatement(
                        "insert into items values (:itemName, :itemPrice)",
                        mapOf(
                            "itemName" to JsonPrimitive("Name"),
                            "itemPrice" to JsonPrimitive(42),
                        )
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
        val items = database.executeQuery("select * from items") {
            mapOf(
                "name" to getString("name"),
                "price" to getInt("price"),
            )
        }

        items.size.shouldEqual(1)
        items[0]["name"].shouldEqual("Name")
        items[0]["price"].shouldEqual(42)
    }


    @Test
    fun `executes multiple statements in transaction`() {
        //given
        database.executeChanges(

            "drop table if exists items",
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
            connection = connection
        )

        //when
        val result = runCatching {
            handler.handleRequest(
                event,
                logger
            )
        }

        //then
        result.isFailure.shouldEqual(true)
        val items = database.executeQuery("select id from items") {
            getString("id")
        }

        items.size.shouldEqual(0)
    }

    @Test
    fun `can select values`() {
        //given
        database.executeChanges(

            "drop table if exists items",
            "create table items (id varchar(100) primary key, name text, price int)",
            "insert into items values ('one', 'One', 10)",
            "insert into items values ('two', 'Two', 20)"
        )

        val event = newCreateEvent(
            CfnSqlRuns(
                listOf(
                    CfnSqlStatement(
                        """select id as "Id", name as "Name", price as "Price" from items"""
                    )
                )
            ),
            connection = connection
        )

        //when
        val result = runCatching {
            handler.handleRequest(
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
        database.executeChanges(

            "drop table if exists items",
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
            connection = connection
        )
        handler.handleRequest(create, logger)


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
            connection
        )
        handler.handleRequest(delete, logger)

        //then
        val items = database.executeQuery("select id from items") {
            getString("id")
        }

        items.size.shouldEqual(0)
    }

    @Test
    fun `update event executes previous down`() {
        //given
        database.executeChanges(

            "drop table if exists items",
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
            connection = connection
        )
        handler.handleRequest(create, logger)


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
            connection = connection,
        )
        handler.handleRequest(update, logger)

        //then
        val items = database.executeQuery("select id from items") {
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
        LoggerFactory.getLogger(javaClass).debug("executeQuery {}", query)
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
        LoggerFactory.getLogger(javaClass).debug("executeChange {}", query)
        it.prepareStatement(query).executeUpdate()
    }
}

fun JdbcDatabaseContainer<out JdbcDatabaseContainer<*>>.executeChanges(vararg queries: String): List<Int> {
    return queries.map { executeChange(it) }
}

fun newMysqlDriverTypeHostPort() = newSqlRunConnectionForDriverType(ConnectionDriverType.mysql)

fun postgresqlDriverTypeHostPort() = newSqlRunConnectionForDriverType(ConnectionDriverType.postgresql)


internal fun newSqlRunConnectionForDriverType(connectionDriverType: ConnectionDriverType) =
    CfnSqlRunConnection.DriverTypeHostPort(
        driverType = connectionDriverType,
        username = "test-username",
        password = "test-password",
        database = "test-database",
        host = "database",
        port = connectionDriverType.defaultPort
    )
