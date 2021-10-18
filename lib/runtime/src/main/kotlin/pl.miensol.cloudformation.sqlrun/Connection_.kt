package pl.miensol.cloudformation.sqlrun

import java.math.BigDecimal
import java.sql.Connection
import java.sql.PreparedStatement

internal fun <T> Connection.inTransactionDo(function: () -> T): T {
    val oldAutoCommit = autoCommit
    autoCommit = false
    return try {
        function().also {
            commit()
        }
    } catch (ex: Exception) {
        rollback()
        throw ex
    } finally {
        autoCommit = oldAutoCommit
    }
}

fun Connection.prepareStatement(statement: JdbcFormattedSqlStatement): PreparedStatement {
    val sqlStatement = prepareStatement(statement.sql)
    statement.parameters?.forEachIndexed { index, value ->
        val statementParamIndex = index + 1
        when (value) {
            is String -> sqlStatement.setString(statementParamIndex, value)
            is BigDecimal -> sqlStatement.setBigDecimal(statementParamIndex, value)
            is Boolean -> sqlStatement.setBoolean(statementParamIndex, value)
            null -> sqlStatement.setNull(statementParamIndex, java.sql.Types.OTHER)
        }
    }
    return sqlStatement
}

