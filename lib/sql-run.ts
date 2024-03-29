import { CustomResource } from "aws-cdk-lib";
import { Construct } from 'constructs';
import { isSqlSecret } from "./secret";
import { SqlRunProvider } from "./sql-run-provider";
import { CfnSqlRunProps, SqlRunProps, toCfnSqlRunProps } from "./sql-run-types";

export class SqlRun extends Construct {
  private customResource: CustomResource;
  private provider: SqlRunProvider;
  private properties: CfnSqlRunProps

  constructor(scope: Construct, id: string, props: SqlRunProps) {
    super(scope, id);
    this.provider = new SqlRunProvider(this, 'Provider', {
      provider: {
        logRetention: props.logRetention
      },
      lambda: {
        logRetention: props.logRetention,
        vpc: props.vpc,
        securityGroups: props.securityGroups
      }
    });

    props.connection.connections?.allowDefaultPortFrom(this.provider.lambda.connections)

    this.properties = toCfnSqlRunProps(props);

    this.grantAccessToSecretParameters(props)

    this.customResource = new CustomResource(this, 'Resource', {
      serviceToken: this.provider.serviceToken,
      resourceType: 'Custom::SqlRun',
      properties: this.properties
    })
  }

  getSingleStatementResult(columnName: string, row: number = 0): string{
    return this.getStatementResult(0, columnName, row)
  }

  getStatementResult(statementIndex: number, columnName: string, row: number = 0): string{
    return this.customResource.getAttString(`${statementIndex}.${row}.${columnName}`)
  }

  get connections(){
    return this.provider.lambda.connections
  }

  get providerLambdaRole(){
    return this.provider.executionRole;
  }

  private grantAccessToSecretParameters(sqlRunProperties: SqlRunProps) {
    const allParameters = sqlRunProperties.up.run.concat(sqlRunProperties.down?.run ?? [])
      .map(statement => statement.parameters);

    for (const parameters of allParameters) {
      for (const key in parameters) {
        const value = parameters[key]
        if (isSqlSecret(value)) {
          value.grantRead(this.provider.executionRole)
        }
      }
    }

    const connectionPassword = sqlRunProperties.connection.toCfnSqlRunConnection().password;
    if (isSqlSecret(connectionPassword)) {
      connectionPassword.grantRead(this.provider.executionRole)
    }
  }
}

