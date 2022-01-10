package pl.miensol.cloudformation.sqlrun

internal fun newUpdateEvent(
    up: CfnSqlRuns = CfnSqlRuns(emptyList()),
    down: CfnSqlRuns = CfnSqlRuns(emptyList()),
    previous: CustomResourceEvent,
    connection: CfnSqlRunConnection
) = CustomResourceEvent.Update(
    serviceToken = "test.serviceToken",
    responseURL = "test.responseURL",
    stackId = "test.stackId",
    requestId = "test.requestId",
    logicalResourceId = "test.logicalResourceId",
    resourceType = "test.resourceType",
    physicalResourceId = "test.physicalResourceId",
    resourceProperties = CfnSqlRunProps(
        ServiceToken = "test.serviceToken",
        connection = connection,
        up = up,
        down = down,
    ),
    oldResourceProperties = previous.resourceProperties,
    requestType = "Update"
)