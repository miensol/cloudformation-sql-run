import * as ec2 from '@aws-cdk/aws-ec2';
import * as rds from "@aws-cdk/aws-rds";
import { DatabaseInstanceEngine, MysqlEngineVersion } from "@aws-cdk/aws-rds";
import * as secretmanager from '@aws-cdk/aws-secretsmanager';
import * as cdk from "@aws-cdk/core";
import { RemovalPolicy } from "@aws-cdk/core";
import { SqlRun, SqlRunConnection, SqlSecret } from "cloudformation-sql-run";

export class ExamplesStack extends cdk.Stack {
  constructor(scope: cdk.Construct, id: string, props?: cdk.StackProps) {
    super(scope, id, props);

    const password = new secretmanager.Secret(this, 'Some Password')

    const vpc = new ec2.Vpc(this, 'vpc', {
      natGateways: 1
    })

    const db = new rds.DatabaseInstance(this, 'db', {
      databaseName: 'sqlrunexample',
      engine: DatabaseInstanceEngine.mysql({
        version: MysqlEngineVersion.VER_8_0
      }),
      vpc: vpc,
      removalPolicy: RemovalPolicy.DESTROY
    })

    const createDatabaseUser = new SqlRun(this, 'Create Database User', {
      vpc: vpc,
      connection: SqlRunConnection.fromDatabaseInstance(db),
      up: {
        run: [{
          sql: `CREATE USER 'myUser'@'%' IDENTIFIED BY :password`,
          parameters: {
            password: SqlSecret.fromSecretsManager(password)
          }
        }, {
          sql: `GRANT ALL ON sqlrunexample.* TO 'myUser'@'%'`,
        }, {
          sql: `FLUSH privileges`,
        }],
      },
      down: {
        run: [{
          sql: `DROP USER 'myUser'@'%'`
        }]
      }
    });



  }
}
