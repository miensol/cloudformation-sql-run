package pl.miensol.cloudformation.sqlrun

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class CfnSqlRunConnection {
    abstract val username: String
    abstract val password: String

    @Serializable
    @SerialName("driverTypeHostPort")
    data class DriverTypeHostPort(
        val driverType: ConnectionDriverType,
        override val username: String,
        override val password: String,
        val database: String,
        val host: String,
        val port: Int
    ) : CfnSqlRunConnection()
}