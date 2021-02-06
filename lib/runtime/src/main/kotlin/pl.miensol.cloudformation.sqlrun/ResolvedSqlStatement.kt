package pl.miensol.cloudformation.sqlrun

class ResolvedSqlStatement(
    override val sql: String,
    override val parameters: Map<String, Any?>?
) : SqlStatement