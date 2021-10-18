package pl.miensol.cloudformation.sqlrun

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.LambdaLogger
import com.amazonaws.services.lambda.runtime.RequestStreamHandler
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream
import java.sql.Connection


internal class Handler(
    private val resolver: ParameterReferenceResolver = awsDynamicReferencesResolver(),
    private val connectionFactory: DatabaseConnectionFactory = DatabaseConnectionFactory(resolver)
) : RequestStreamHandler {
    override fun handleRequest(input: InputStream, output: OutputStream, context: Context) {
        val log = context.logger
        input.use {
            output.use {
                log.log("Start reading input")
                val readJson = input.bufferedReader().readText()
                log.log("Done reading input. Json = $readJson")
                val event = Json.decodeFromString(CustomResourceEvent.serializer(), readJson)
                log.log("Decoded event=$event")

                val result = handleRequest(event, log)

                log.log("Start writing output. Result=$result")
                val resultJson = Json.encodeToString(CloudFormationCustomResourceResponseCommon.serializer(), result)
                output.bufferedWriter().use { it.write(resultJson) }
                log.log("Start writing output.")
            }
        }
    }

    internal fun handleRequest(
        event: CustomResourceEvent,
        log: LambdaLogger
    ): CloudFormationCustomResourceResponseCommon {
        return when (event) {
            is CustomResourceEvent.Create -> {
                val result = connectionFactory.open(event.resourceProperties.connection).use { connection ->
                    connection.inTransactionDo {
                        event.resourceProperties.up.run.map {
                            executeStatement(connection, it, resolver, log)
                        }
                    }
                }
                log.log("Done handling Create result=$result")
                CloudFormationCustomResourceResponseCommon(
                    event, result
                )
            }
            is CustomResourceEvent.Update -> {
                val result = connectionFactory.open(event.resourceProperties.connection).use { connection ->
                    connection.inTransactionDo {
                        event.oldResourceProperties.down?.run?.forEach {
                            executeStatement(connection, it, resolver, log)
                        }
                        event.resourceProperties.up.run.map {
                            executeStatement(connection, it, resolver, log)
                        }
                    }
                }
                log.log("Done handling Update result=$result")
                CloudFormationCustomResourceResponseCommon(
                    event, result
                )
            }
            is CustomResourceEvent.Delete -> {
                val result = connectionFactory.open(event.resourceProperties.connection).use { connection ->
                    connection.inTransactionDo {
                        event.resourceProperties.down?.run?.map {
                            executeStatement(connection, it, resolver, log)
                        }
                    }
                }
                log.log("Done handling Delete result=$result")
                CloudFormationCustomResourceResponseCommon(
                    event, result
                )
            }
        }

    }
}

internal fun executeStatement(
    connection: Connection,
    statement: CfnSqlStatement,
    resolver: ParameterReferenceResolver,
    log: LambdaLogger
): List<Map<String, Any?>> {
    val unresolvedSqlStatement = statement.toSqlStatement()
    log.log("Running $unresolvedSqlStatement")
    val resolvedSqlStatement = unresolvedSqlStatement.resolveParameters(resolver)
    val formattedStatement = resolvedSqlStatement.toJdbcFormattedSqlStatement()
    val sqlStatement = connection.prepareStatement(formattedStatement)
    val hasResultSet = sqlStatement.execute()
    return if (hasResultSet) {
        sqlStatement.resultSet.use { results ->
            val meta = results.metaData
            val columnsCount = meta.columnCount
            sequence<Map<String, Any?>> {
                while (results.next()) {
                    yield(
                        (1..columnsCount).associateBy(
                            { meta.getColumnLabel(it) },
                            { results.getObject(it) }
                        )
                    )
                }
            }.toList()

        }
    } else {
        listOf(mapOf("count" to sqlStatement.updateCount))
    }
}
