import { Template } from 'aws-cdk-lib/assertions';
import * as cdk from "aws-cdk-lib";
import * as Examples from '../lib/examples-stack';

test('Empty Stack', () => {
    const app = new cdk.App();
    // WHEN
    const stack = new Examples.ExamplesStack(app, 'MyTestStack');
    // THEN
    // Template.fromStack(stack).(matchTemplate({
    //   "Resources": {}
    // }, MatchStyle.EXACT))
});
