import * as logs from "@aws-cdk/aws-logs";
import * as ec2 from "@aws-cdk/aws-ec2";
import { SecretValue } from "@aws-cdk/core";
import { CfnSqlRunConnection, SqlRunConnection } from "./connection-types";
import { isSqlSecret, SqlSecret } from "./secret";

type SqlParameterType = string | number | SqlSecret
type CfnSqlParameterType = string | number | SecretValue

interface SqlStatement {
  sql: string
  parameters?: {
    [name: string]: SqlParameterType
  }
}

interface CfnSqlStatement {
  sql: string
  parameters?: {
    [name: string]: CfnSqlParameterType
  }
}

interface SqlRuns {
  run: SqlStatement[]
}

interface CfnSqlRuns {
  run: CfnSqlStatement[]
}

export interface SqlRunProps {
  vpc?: ec2.IVpc
  securityGroups?: ec2.ISecurityGroup[]
  connection: SqlRunConnection
  up: SqlRuns
  down?: SqlRuns
  logRetention?: logs.RetentionDays
}

export interface CfnSqlRunProps {
  connection: CfnSqlRunConnection
  up: CfnSqlRuns
  down?: CfnSqlRuns
}

function toCfnSqlStatement(sqlStatement: SqlStatement): CfnSqlStatement {
  const parameters = sqlStatement.parameters;
  return {
    sql: sqlStatement.sql,
    parameters: parameters ? Object.keys(parameters).reduce((acc, key) => {
      const value = parameters[key];

      if (isSqlSecret(value)) {
        acc[key] = value.value
      } else {
        acc[key] = value
      }

      return acc;
    }, {} as Exclude<CfnSqlStatement['parameters'], undefined>) : undefined
  }
}

export function toCfnSqlRunProps(sqlProps: SqlRunProps): CfnSqlRunProps {
  return {
    connection: sqlProps.connection.toCfnSqlRunConnection(),
    up: {
      run: sqlProps.up.run.map(st => toCfnSqlStatement(st))
    },
    down: sqlProps.down ? {
      run: sqlProps.down.run.map(st => toCfnSqlStatement(st))
    } : undefined
  }
}
