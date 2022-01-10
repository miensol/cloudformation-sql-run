package pl.miensol.cloudformation.sqlrun

import io.mockk.every
import io.mockk.mockk
import org.testcontainers.containers.PostgreSQLContainerProvider
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
internal class HandlerPostgresqlTestTests : ItemsTableSqlTests {
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
        val postgresql = PostgreSQLContainerProvider().newInstance()
            .withUsername("root")
            .withPassword("")
            .withReuse(true)
            .withDatabaseName("test")
    }

    override val database = postgresql

    override val connection
        get() = postgresqlDriverTypeHostPort()
            .copy(
                username = database.username,
                password = database.password,
                host = "localhost",
                port = database.firstMappedPort,
                database = database.databaseName,
                options = mapOf(
                )
            )
}