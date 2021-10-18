package pl.miensol.cloudformation.sqlrun

class JdbcFormattedSqlStatement(
    val sql: String,
    val parameters: List<Any?>?
)

fun SqlStatement.toJdbcFormattedSqlStatement(): JdbcFormattedSqlStatement {
    var formatted = sql

    val parametersList = mutableListOf<Any?>()

    parameters?.forEach { (name, value) ->
        formatted = formatted.replace(":${name}", "?")
        parametersList += value
    }

    return JdbcFormattedSqlStatement(
        formatted,
        parametersList
    )
}