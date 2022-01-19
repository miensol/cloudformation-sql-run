import * as iam from "aws-cdk-lib/aws-iam";
import { ISecret } from "aws-cdk-lib/aws-secretsmanager";
import { IResolvable, IResolveContext, SecretValue } from "aws-cdk-lib";
import { IStringParameter } from "aws-cdk-lib/aws-ssm";

const SqlSecretSymbol = Symbol("SqlSecret")

export abstract class SqlSecret implements IResolvable {
  private kind = SqlSecretSymbol
  abstract readonly value: SecretValue

  abstract grantRead(grantee: iam.IGrantable): iam.Grant

  toJSON() {
    return this.value.toJSON()
  }

  get creationStack() {
    return this.value.creationStack
  }

  resolve(context: IResolveContext): any {
    return this.value.resolve(context)
  }

  static fromSecretsManager(secret: ISecret, field?: string): SqlSecret {
    return new class extends SqlSecret {
      readonly value: SecretValue = field ? secret.secretValueFromJson(field) : secret.secretValue;
      grantRead = (grantee: iam.IGrantable) => secret.grantRead(grantee)
    }
  }

  static fromSSMParameter(secret: IStringParameter, version?: string): SqlSecret {
    return new class extends SqlSecret {
      readonly value: SecretValue = SecretValue.ssmSecure(secret.parameterName, version)
      grantRead = (grantee: iam.IGrantable) => secret.grantRead(grantee)
    }
  }
}

export function isSqlSecret(value: any): value is SqlSecret {
  return typeof value === 'object' && 'kind' in value && value.kind == SqlSecretSymbol
}
