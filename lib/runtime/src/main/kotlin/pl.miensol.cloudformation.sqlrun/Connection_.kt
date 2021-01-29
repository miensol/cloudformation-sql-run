package pl.miensol.cloudformation.sqlrun

import kotlinx.serialization.json.JsonNull
import java.sql.Connection
import java.sql.PreparedStatement

internal fun Connection.inTransactionDo(function: () -> Unit) {
    autoCommit = false
    try {
        function()
        commit()
    } catch (ex: Exception) {
        rollback()
        throw ex
    }
}

fun Connection.prepareStatement(statement: CfnSqlStatement): PreparedStatement {
    val sqlStatement = prepareStatement(statement.jdbcFormattedSql)
    statement.jdbcParameters.forEachIndexed { index, value ->
        val statementParamIndex = index + 1
        when {
            value.isString -> sqlStatement.setString(statementParamIndex, value.content)
            value.content == "true" || value.content == "false" -> sqlStatement.setBoolean(
                statementParamIndex,
                value.content.toBoolean()
            )
            value.content.toBigDecimalOrNull() != null -> sqlStatement.setBigDecimal(
                statementParamIndex,
                value.content.toBigDecimal()
            )
            value is JsonNull -> sqlStatement.setNull(statementParamIndex, java.sql.Types.OTHER)
        }
    }
    return sqlStatement
}

