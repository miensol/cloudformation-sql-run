package pl.miensol.cloudformation.sqlrun

import java.sql.Driver


enum class ConnectionDriverType(val defaultPort: Int,
                                val urlScheme: String,
                                val loadDriver: () -> Driver) {
    mysql(defaultPort = 3306, "mariadb", LoadMysqlCompatibleDriver),
    postgresql(defaultPort = 5432, "postgresql", LoadPostgresqlCompatibleDriver)
}


val LoadMysqlCompatibleDriver = {
    @Suppress("UNCHECKED_CAST")
    val driverClass: Class<Driver> =
        Class.forName("org.mariadb.jdbc.Driver") as Class<Driver>
    driverClass.getDeclaredConstructor().newInstance()
}

val LoadPostgresqlCompatibleDriver = {
    @Suppress("UNCHECKED_CAST")
    val driverClass: Class<Driver> =
        Class.forName("org.postgresql.Driver") as Class<Driver>
    driverClass.getDeclaredConstructor().newInstance()
}