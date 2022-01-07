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
        val options = connection.options.map { (name, value) -> "${name}=${value}" }.joinToString("&")
        val url = "jdbc:${connection.driverType.urlScheme}://${connection.host}:${connection.port}/${connection.database}?${options}"
        return driver.connect(url, Properties().apply {
            setProperty("user", connection.username)
            setProperty("password", connection.password)
            setProperty("useSSL", "true")
        })
    }
}
