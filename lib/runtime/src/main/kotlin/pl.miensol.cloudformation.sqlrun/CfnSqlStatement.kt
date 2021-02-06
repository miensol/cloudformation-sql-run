package pl.miensol.cloudformation.sqlrun

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive

@Serializable
internal data class CfnSqlStatement(
    val sql: String,
    val parameters: Map<String, JsonPrimitive>? = null
) {
    fun toSqlStatement() = UnresolvedSqlStatement(
        sql,
        parameters = parameters?.mapValues { (_, value) ->
            when {
                value.isString -> value.content
                value.content == "true" || value.content == "false" ->
                    value.content.toBoolean()
                value.content.toBigDecimalOrNull() != null -> value.content.toBigDecimal()
                value is JsonNull -> null
                else -> throw IllegalArgumentException("Unsupported value type: $value")
            }
        }
    )
}