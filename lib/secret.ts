import * as iam from "@aws-cdk/aws-iam";
import { ISecret } from "@aws-cdk/aws-secretsmanager";
import { IResolvable, IResolveContext, SecretValue } from "@aws-cdk/core";
import { privateDecrypt } from "crypto";

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
}

export function isSqlSecret(value: any): value is SqlSecret {
  return 'kind' in value && value.kind == SqlSecretSymbol
}
