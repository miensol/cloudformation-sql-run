import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as rds from "aws-cdk-lib/aws-rds";
import {
  CfnDBInstance,
  DatabaseInstanceEngine,
  PostgresEngineVersion
} from "aws-cdk-lib/aws-rds";
import { DEFAULT_PASSWORD_EXCLUDE_CHARS } from "aws-cdk-lib/aws-rds/lib/private/util";
import * as secretmanager from 'aws-cdk-lib/aws-secretsmanager';
import * as cdk from "aws-cdk-lib";
import { RemovalPolicy } from "aws-cdk-lib";
import { SqlRun, SqlRunConnection, SqlSecret } from "cloudformation-sql-run";
import { Construct } from "constructs";

export class ExamplesStack extends cdk.Stack {
  constructor(scope: Construct, id: string, props?: cdk.StackProps) {
    super(scope, id, props);


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

    const cfnDatabase: CfnDBInstance = db.node.defaultChild as CfnDBInstance;

    const password = new secretmanager.Secret(this, 'Replication User Password', {
      generateSecretString: {
        excludeCharacters: DEFAULT_PASSWORD_EXCLUDE_CHARS
      }
    })


    const createReplicationUser = new SqlRun(this, 'Create Replication User', {
      vpc: vpc,
      connection: SqlRunConnection.fromDriverTypeHostPort({
        password: SqlSecret.fromSecretsManager(db.secret!, 'password'),
        username: cfnDatabase.masterUsername!,
        driverType: 'postgresql',
        database: cfnDatabase.dbName!,
        host: cfnDatabase.attrEndpointAddress!,
        port: cfnDatabase.attrEndpointPort! as any as number
      }),
      up: {
        run: [{
          sql: `CREATE USER pgrepuser WITH password '${password.secretValue}'`,
          parameters: {
            externalId:
          }
        }, {
          sql: `GRANT rds_replication TO pgrepuser`,
        }, {
          sql: `GRANT SELECT ON ALL TABLES IN SCHEMA public TO pgrepuser`,
        }],
      },
      down: {
        run: [{
          sql: `DROP USER pgrepuser`
        }]
      }
    });

    createReplicationUser.getStatementResult(0)
  }
}
