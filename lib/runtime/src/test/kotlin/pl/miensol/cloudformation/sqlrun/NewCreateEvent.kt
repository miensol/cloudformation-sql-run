package pl.miensol.cloudformation.sqlrun

internal fun newCreateEvent(
    up: CfnSqlRuns,
    down: CfnSqlRuns = CfnSqlRuns(emptyList()),
    connection: CfnSqlRunConnection
) = CustomResourceEvent.Create(
    serviceToken = "test.serviceToken",
    responseURL = "test.responseURL",
    stackId = "test.stackId",
    requestId = "test.requestId",
    logicalResourceId = "test.logicalResourceId",
    resourceType = "test.resourceType",
    resourceProperties = CfnSqlRunProps(
        ServiceToken = "test.serviceToken",
        connection = connection,
        up = up,
        down = down
    ),
    requestType = "Create"
)