package pl.miensol.cloudformation.sqlrun

import java.sql.Connection
import java.util.*

class DatabaseConnectionFactory {
    fun open(connection: CfnSqlRunConnection): Connection {
        return when (connection) {
            is CfnSqlRunConnection.DriverTypeHostPort -> open(connection)
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
