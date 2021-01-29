import * as rds from "@aws-cdk/aws-rds"
import { SqlSecret } from "./secret";

type ConnectionDriverType =
  'mysql'

export interface DriverTypeHostPortSqlRunConnection {
  type: 'driverTypeHostPort'
  driverType: ConnectionDriverType
  username: string
  password: SqlSecret
  database: string
  host: string
  port?: number
}

export interface JdbcUrlSqlRunConnection {
  type: 'jdbcUrl'
  jdbcUrl: string
  username?: string
  password?: string
}

export interface RdsInstanceSqlRunConnection {
  type: 'rds'
  instance: rds.DatabaseInstance
}

export type CfnSqlRunConnection = DriverTypeHostPortSqlRunConnection | JdbcUrlSqlRunConnection

export abstract class SqlRunConnection {
  static fromDriverTypeHostPort(props: Omit<DriverTypeHostPortSqlRunConnection, 'type'>) {
    return new DriverTypeHostPort(props)
  }
}


class DriverTypeHostPort extends SqlRunConnection implements DriverTypeHostPortSqlRunConnection {
  readonly type = "driverTypeHostPort"
  readonly driverType: ConnectionDriverType
  readonly username: string
  readonly password: SqlSecret
  readonly database: string
  readonly host: string
  readonly port?: number

  constructor({
                driverType,
                username,
                password,
                database,
                host,
                port
              }: Omit<DriverTypeHostPortSqlRunConnection, 'type'>) {
    super();
    this.driverType = driverType;
    this.username = username;
    this.password = password;
    this.database = database;
    this.host = host;
    this.port = port;
  }
}
