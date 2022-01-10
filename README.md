# Call SQL during CloudFormation update

An [aws-cdk](https://github.com/aws/aws-cdk) construct for calling SQL during CloudFormation stack
update.

## Usage

The `SqlRun` resource provides 2 callbacks: `up`, `down`.

The `up` callback runs a forward migration. The `down` command should do the opposite of `up`.

The following example will issue a POST request when a `Some API` resource creates:

```typescript
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
```

### SQL results as data

You can access results of the last sql statement as follows:

```typescript
const createDatabaseUser = new SqlRun(this, 'Create Database User', {
  up: { run: [{ sql: `select count(*) as admin_count, company_id from users where is_admin = true group by company_id` }] }
});

new CfnOutput(this, 'Row at index 0', {
  description: "First result",
  value: sampleQuery.getResultField("[0].admin_count")
})

new CfnOutput(this, 'Row at index 1', {
  description: "Second result",
  value: sampleQuery.getResultField("[1].company_id")
})

```
