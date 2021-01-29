# Call SQL during CloudFormation update

An [aws-cdk](https://github.com/aws/aws-cdk) construct for calling SQL during CloudFormation stack
update.

## Usage

The `SqlRun` resource provides 2 callbacks: `up`, `down`.

The `up` callback runs a forward migration. The `down` command should do the opposite of `up`.

The following example will issue a POST request when a `Some API` resource creates:

```typescript
import * as cdk from '@aws-cdk/core';
import { SqlRun } from "cloudformation-sql-run";

export class ExamplesStack extends cdk.Stack {
  constructor(scope: cdk.Construct, id: string, props?: cdk.StackProps) {
    super(scope, id, props);

    const secret: secretmanager.ISecret = getOrCreateSecret()

    const createDatabaseUser = new SqlRun(this, 'Create Database User', {
      up: {
        run: [{
          sql: `CREATE USER 'myDatabaseUser'@'%' IDENTIFIED BY ':password';

GRANT ALL ON relevo.* TO 'myDatabaseUser'@'%';

FLUSH privileges
`,
          parameters: {
            password: secret
          }
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
