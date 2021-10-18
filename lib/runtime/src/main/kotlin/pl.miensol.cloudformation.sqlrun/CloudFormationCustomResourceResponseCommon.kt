package pl.miensol.cloudformation.sqlrun

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import java.util.*

@Serializable
internal data class CloudFormationCustomResourceResponseCommon(
    val PhysicalResourceId: String,
    val StackId: String,
    val RequestId: String,
    val LogicalResourceId: String,
    val Data: JsonElement,
    val NoEcho: Boolean = false
) {
    constructor(input: CustomResourceEvent.Create, data: Any?) : this(
        PhysicalResourceId = UUID.randomUUID().toString(),
        StackId = input.stackId,
        RequestId = input.requestId,
        LogicalResourceId = input.logicalResourceId,
        Data = flattenOrEmpty(data?.toJsonElement())
    )

    constructor(event: CustomResourceEvent.Update, data: Any?) : this(
        PhysicalResourceId = event.physicalResourceId,
        StackId = event.stackId,
        RequestId = event.requestId,
        LogicalResourceId = event.logicalResourceId,
        Data = flattenOrEmpty(data?.toJsonElement())
    )

    constructor(event: CustomResourceEvent.Delete, data: Any?) : this(
        PhysicalResourceId = event.physicalResourceId,
        StackId = event.stackId,
        RequestId = event.requestId,
        LogicalResourceId = event.logicalResourceId,
        Data = flattenOrEmpty(data?.toJsonElement())
    )
}
