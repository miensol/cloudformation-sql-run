#!/usr/bin/env node
import 'source-map-support/register';
import { Tags } from "@aws-cdk/core";
import * as cdk from '@aws-cdk/core';
import { ExamplesStack } from '../lib/examples-stack';

const app = new cdk.App();
new ExamplesStack(app, 'SqlRunExample', {
  env: {
    region: process.env.CDK_DEFAULT_REGION,
    account: process.env.CDK_DEFAULT_ACCOUNT,
  }
});

const appTags = Tags.of(app);
appTags.add("source", "https://github.com/miensol/cloudformation-sql-run")
appTags.add("project", "cloudformation-sql-run")
