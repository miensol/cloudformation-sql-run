package pl.miensol.cloudformation.sqlrun

internal data class UnresolvedSqlStatement(
    override val sql: String,
    override val parameters: Map<String, Any?>?
) : SqlStatement {
    fun resolveParameters(resolver: ParameterReferenceResolver): ResolvedSqlStatement {
        return ResolvedSqlStatement(sql, parameters?.mapValues { (_, value) ->
            when (value) {
                is String -> resolver.resolve(value)
                else -> value
            }
        })
    }
}