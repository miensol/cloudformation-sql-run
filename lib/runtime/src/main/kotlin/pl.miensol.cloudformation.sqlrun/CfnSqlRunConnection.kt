package pl.miensol.cloudformation.sqlrun

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class CfnSqlRunConnection {
    abstract val username: String
    abstract val password: String

    internal abstract fun resolveDynamicParameterReferences(resolver: ParameterReferenceResolver): CfnSqlRunConnection

    @Serializable
    @SerialName("driverTypeHostPort")
    data class DriverTypeHostPort(
        val driverType: ConnectionDriverType,
        override val username: String,
        override val password: String,
        val database: String,
        val host: String,
        val port: Int
    ) : CfnSqlRunConnection() {
        override fun resolveDynamicParameterReferences(resolver: ParameterReferenceResolver) =
            copy(password = resolver.resolve(password))
    }
}