import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as rds from "aws-cdk-lib/aws-rds";
import {
  DatabaseInstanceEngine,
  PostgresEngineVersion
} from "aws-cdk-lib/aws-rds";
import * as secretmanager from 'aws-cdk-lib/aws-secretsmanager';
import * as cdk from "aws-cdk-lib";
import { RemovalPolicy } from "aws-cdk-lib";
import { SqlRun, SqlRunConnection, SqlSecret } from "cloudformation-sql-run";
import { Construct } from "constructs";

export class ExamplesStack extends cdk.Stack {
  constructor(scope: Construct, id: string, props?: cdk.StackProps) {
    super(scope, id, props);

    const password = new secretmanager.Secret(this, 'Some Password')

    const vpc = new ec2.Vpc(this, 'vpc', {
      natGateways: 1
    })

    const db = new rds.DatabaseInstance(this, 'db', {
      databaseName: 'sqlrunexample',
      engine: DatabaseInstanceEngine.postgres({
        version: PostgresEngineVersion.VER_10
      }),
      vpc: vpc,
      removalPolicy: RemovalPolicy.DESTROY
    })

    const createDatabaseUser = new SqlRun(this, 'Create Database User', {
      vpc: vpc,
      connection: SqlRunConnection.fromDatabaseInstance(db),
      up: {
        run: [{
          sql: `CREATE TABLE items(name varchar)`
        }, {
          sql: `INSERT INTO items(name) VALUE (:secret)`,
          parameters: {
            secret: SqlSecret.fromSecretsManager(password)
          }
        }],
      },
      down: {
        run: [{
          sql: `DROP TABLE items`
        }]
      }
    });
  }
}
