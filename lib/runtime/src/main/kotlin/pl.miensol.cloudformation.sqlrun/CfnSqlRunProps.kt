package pl.miensol.cloudformation.sqlrun

import kotlinx.serialization.Serializable

@Serializable
data class CfnSqlRunProps(
    val ServiceToken: String? = null, // not used directly
    val connection: CfnSqlRunConnection,
    val up: CfnSqlRuns,
    val down: CfnSqlRuns? = null
)