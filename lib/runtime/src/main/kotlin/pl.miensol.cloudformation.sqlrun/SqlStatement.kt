package pl.miensol.cloudformation.sqlrun

interface SqlStatement {
    val sql: String
    val parameters: Map<String, Any?>?
}