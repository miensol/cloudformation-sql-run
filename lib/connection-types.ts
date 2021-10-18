import { Connections } from "@aws-cdk/aws-ec2";
import {
  CfnDBCluster,
  CfnDBInstance,
  DatabaseCluster,
  DatabaseInstance,
  IDatabaseInstance
} from "@aws-cdk/aws-rds";
import * as rds from "@aws-cdk/aws-rds"
import { SqlSecret } from "./secret";

type ConnectionDriverType = 'mysql'

export interface DriverTypeHostPortSqlRunConnection {
  type: 'driverTypeHostPort'
  driverType: ConnectionDriverType
  username: string
  password: SqlSecret
  database: string
  host: string
  port?: number | string
}

export interface JdbcUrlSqlRunConnection {
  type: 'jdbcUrl'
  jdbcUrl: string
  username?: string
  password?: string
}

export type CfnSqlRunConnection = DriverTypeHostPortSqlRunConnection | JdbcUrlSqlRunConnection

export abstract class SqlRunConnection {
  abstract toCfnSqlRunConnection(): CfnSqlRunConnection

  get connections(): Connections | undefined {
    return undefined;
  }

  static fromDriverTypeHostPort(props: Omit<DriverTypeHostPortSqlRunConnection, 'type'>) {
    return new DriverTypeHostPort(props)
  }

  static fromDatabaseInstance(db: rds.IDatabaseInstance) {
    return new DatabaseInstanceConnection(db)
  }

  static fromDatabaseCluster(db: rds.IDatabaseCluster) {
    return new DatabaseClusterConnection(db)
  }
}


class DriverTypeHostPort extends SqlRunConnection implements DriverTypeHostPortSqlRunConnection {
  readonly type = "driverTypeHostPort"
  readonly driverType: ConnectionDriverType
  readonly username: string
  readonly password: SqlSecret
  readonly database: string
  readonly host: string
  readonly port?: number | string

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

  toCfnSqlRunConnection(): CfnSqlRunConnection {
    return this;
  }
}


class DatabaseInstanceConnection extends SqlRunConnection implements DriverTypeHostPortSqlRunConnection {
  readonly type = "driverTypeHostPort"
  readonly driverType: ConnectionDriverType
  readonly username: string
  readonly password: SqlSecret
  readonly database: string
  readonly host: string
  readonly port?: number | string

  constructor(private readonly db: rds.IDatabaseInstance) {
    super();
    const cfnDbInstance = db.node.defaultChild as CfnDBInstance;
    this.database = cfnDbInstance.dbName!;
    this.driverType = "mysql";
    this.host = db.dbInstanceEndpointAddress;
    this.password = SqlSecret.fromSecretsManager((db as DatabaseInstance).secret!, 'password');
    this.username = cfnDbInstance.masterUsername!;
    this.port = db.dbInstanceEndpointPort;
  }

  toCfnSqlRunConnection(): DriverTypeHostPortSqlRunConnection {
    return {
      type: this.type,
      driverType: this.driverType,
      username: this.username,
      password: this.password,
      database: this.database,
      host: this.host,
      port: this.port,
    }
  }

  get connections(): Connections {
    return this.db.connections;
  }
}


class DatabaseClusterConnection extends SqlRunConnection implements DriverTypeHostPortSqlRunConnection {
  readonly type = "driverTypeHostPort"
  readonly driverType: ConnectionDriverType
  readonly username: string
  readonly password: SqlSecret
  readonly database: string
  readonly host: string
  readonly port?: number | string

  constructor(private readonly db: rds.IDatabaseCluster) {
    super();
    const cfnDBCluster = db.node.defaultChild as CfnDBCluster;
    this.database = cfnDBCluster.databaseName!;
    this.driverType = "mysql";
    this.host = db.clusterEndpoint.hostname;
    const secret = (db as DatabaseCluster).secret;
    this.password = SqlSecret.fromSecretsManager(secret!, 'password');
    this.username = cfnDBCluster.masterUsername!;
    this.port = db.clusterEndpoint.port;
  }

  toCfnSqlRunConnection(): DriverTypeHostPortSqlRunConnection {
    return {
      type: this.type,
      driverType: this.driverType,
      username: this.username,
      password: this.password,
      database: this.database,
      host: this.host,
      port: this.port,
    }
  }

  get connections(): Connections {
    return this.db.connections;
  }
}
