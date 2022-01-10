package pl.miensol.cloudformation.sqlrun

internal fun newDeleteEvent(
    up: CfnSqlRuns = CfnSqlRuns(emptyList()),
    down: CfnSqlRuns,
    connection: CfnSqlRunConnection
) = CustomResourceEvent.Delete(
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
    requestType = "Delete"
)