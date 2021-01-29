package pl.miensol.cloudformation.sqlrun

import java.sql.Driver


enum class ConnectionDriverType(val defaultPort: Int, val loadDriver: () -> Driver) {
    mysql(defaultPort = 3306, LoadMysqlCompatibleDriver)
}


val LoadMysqlCompatibleDriver = {
    @Suppress("UNCHECKED_CAST")
    val driverClass: Class<Driver> =
        Class.forName("org.mariadb.jdbc.Driver") as Class<Driver>
    driverClass.getDeclaredConstructor().newInstance()
}