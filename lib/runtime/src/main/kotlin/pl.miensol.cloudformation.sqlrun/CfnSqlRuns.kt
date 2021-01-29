package pl.miensol.cloudformation.sqlrun

import kotlinx.serialization.Serializable

@Serializable
data class CfnSqlRuns(
    val run: List<CfnSqlStatement>
)