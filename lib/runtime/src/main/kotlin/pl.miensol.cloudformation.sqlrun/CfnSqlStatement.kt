package pl.miensol.cloudformation.sqlrun

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive

@Serializable
data class CfnSqlStatement(
    val sql: String,
    val parameters: Map<String, JsonPrimitive>? = null
) {
    private class JdbcFormatted(val sql: String, val parameters: List<JsonPrimitive>)

    private val jdbcFormatted by lazy {
        var formatted = sql

        val parametersList = mutableListOf<JsonPrimitive>()

        parameters?.forEach { (name, value) ->
            formatted = sql.replace(":${name}", "?")
            parametersList += value
        }

        JdbcFormatted(formatted, parametersList)
    }

    val jdbcFormattedSql get() = jdbcFormatted.sql
    val jdbcParameters get() = jdbcFormatted.parameters
}