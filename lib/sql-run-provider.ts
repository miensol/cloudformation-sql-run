import * as lambda from "aws-cdk-lib/aws-lambda";
import { Duration, Stack } from "aws-cdk-lib";
import * as cr from "aws-cdk-lib/custom-resources";
import { Provider, ProviderProps } from "aws-cdk-lib/custom-resources";
import { Construct, Node } from "constructs";
import { join as joinPath } from "path";

interface SqlRunProviderProps {
  provider?: Omit<ProviderProps, 'onEventHandler'>
  lambda?: lambda.FunctionOptions
}

export class SqlRunProvider extends Construct {
  private provider: Provider;
  readonly lambda: lambda.Function;

  constructor(scope: Construct, id: string, props?: SqlRunProviderProps) {
    super(scope, id);

    let handlerPath = joinPath(__dirname, 'runtime', 'build', 'distributions', 'sql-run-lamba-runtime-1.0.0.zip');

    this.lambda = new lambda.Function(this, 'Lambda', {
      runtime: lambda.Runtime.JAVA_11,
      memorySize: 1024,
      code: lambda.Code.fromAsset(handlerPath),
      handler: 'pl.miensol.cloudformation.sqlrun.Handler',
      timeout: Duration.minutes(1),
      ...props?.lambda
    })

    this.provider = new cr.Provider(this, 'SqlRun', {
      onEventHandler: this.lambda,
      ...props?.provider
    })
  }

  get serviceToken() {
    return this.provider.serviceToken
  }

  get executionRole() {
    const role = this.lambda.role;
    if (!role) {
      throw new Error('No lambda.role defined')
    }
    return role
  }

  public static getOrCreateProvider(scope: Construct) {
    const stack = Stack.of(scope);
    const id = 'pl.miensol.cdk.custom-resources.sql-run';
    const x = scope.node.tryFindChild(id) as SqlRunProvider || new SqlRunProvider(stack, id);
    return x.provider;
  }
}
