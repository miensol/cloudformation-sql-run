package pl.miensol.cloudformation.sqlrun

import kotlinx.serialization.Serializable

@Serializable
internal data class CfnSqlRuns(
    val run: List<CfnSqlStatement>
)