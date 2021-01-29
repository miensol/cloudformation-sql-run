import * as cdk from '@aws-cdk/core';
import * as ec2 from '@aws-cdk/aws-ec2';
import * as secretmanager from '@aws-cdk/aws-secretsmanager';
import * as rds from '@aws-cdk/aws-rds';
import { SqlRun, SqlRunConnection, SqlSecret } from "cloudformation-sql-run";

export class ExamplesStack extends cdk.Stack {
  constructor(scope: cdk.Construct, id: string, props?: cdk.StackProps) {
    super(scope, id, props);

    const password = new secretmanager.Secret(this, 'Some Password')

    // rds.DatabaseInstance.fromDatabaseInstanceAttributes(this, 'SampleDb', {
    //   engine: rds.DatabaseInstanceEngine.mysql({
    //     version: rds.MysqlEngineVersion.VER_8_0_21
    //   }),
    //   instanceEndpointAddress: 'development-backend.cbjzuknka2om.eu-west-1.rds.amazonaws.com',
    //   port: 3306,
    // })

    const vpc = ec2.Vpc.fromLookup(this, 'Vpc', {
      tags: {
        deployEnv: 'development'
      }
    })

    const securityGroup = ec2.SecurityGroup.fromLookup(this, 'DatabaseSecurityGroup', 'sg-0dccf3c2c3d541555')

    const createDatabaseUser = new SqlRun(this, 'Create Database User', {
      vpc: vpc,
      securityGroups: [securityGroup],
      connection: SqlRunConnection.fromDriverTypeHostPort({
        database: 'relevo',
        driverType: 'mysql',
        host: 'development-backend.cbjzuknka2om.eu-west-1.rds.amazonaws.com',
        port: 3306,
        username: 'relevo_admin',
        password: SqlSecret.fromSecretsManager(
          secretmanager.Secret.fromSecretNameV2(this, 'ImportedAdminPassword', 'developmentDatabaseSecretC0-RHxIoHua2RnU'),
          'password'
        )
      }),
      up: {
        run: [{
          sql: `CREATE USER 'myDatabaseUser'@'%' IDENTIFIED BY ':password'`,
          parameters: {
            password: SqlSecret.fromSecretsManager(password)
          }
        }, {
          sql: `GRANT ALL ON relevo.* TO 'myDatabaseUser'@'%'`,
        }, {
          sql: `FLUSH privileges`,
        }],
      },
      down: {
        run: [{
          sql: `DROP USER 'myDatabaseUser'@'%'`
        }]
      }
    });

  }
}
