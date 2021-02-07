package pl.miensol.cloudformation.sqlrun

import java.sql.Connection
import java.util.*

internal class DatabaseConnectionFactory(private val resolver: ParameterReferenceResolver) {
    fun open(connection: CfnSqlRunConnection): Connection {
        return when (val resolvedConnection = connection.resolveDynamicParameterReferences(resolver)) {
            is CfnSqlRunConnection.DriverTypeHostPort -> open(resolvedConnection)
        }
    }

    private fun open(connection: CfnSqlRunConnection.DriverTypeHostPort): Connection {
        val driver = connection.driverType.loadDriver()
        val url = "jdbc:mariadb://${connection.host}:${connection.port}/${connection.database}"
        return driver.connect(url, Properties().apply {
            setProperty("user", connection.username)
            setProperty("password", connection.password)
        })
    }
}
