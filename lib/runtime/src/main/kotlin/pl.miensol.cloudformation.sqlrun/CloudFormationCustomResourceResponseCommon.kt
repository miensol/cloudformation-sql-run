package pl.miensol.cloudformation.sqlrun

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import java.util.*

@Serializable
internal data class CloudFormationCustomResourceResponseCommon(
    val physicalResourceId: String,
    val stackId: String,
    val requestId: String,
    val logicalResourceId: String,
    val data: String?,
    val noEcho: Boolean = false
) {
    constructor(input: CustomResourceEvent.Create, data: Map<String, Any?>) : this(
        physicalResourceId = UUID.randomUUID().toString(),
        stackId = input.stackId,
        requestId = input.requestId,
        logicalResourceId = input.logicalResourceId,
        data = Json.encodeToString(JsonObject.serializer(), data.toJsonObject())
    )

    constructor(event: CustomResourceEvent.Update, data: Map<String, Any?>) : this(
        physicalResourceId = event.physicalResourceId,
        stackId = event.stackId,
        requestId = event.requestId,
        logicalResourceId = event.logicalResourceId,
        data = Json.encodeToString(JsonObject.serializer(), data.toJsonObject())
    )

    constructor(event: CustomResourceEvent.Delete, data: Map<String, Any?>) : this(
        physicalResourceId = event.physicalResourceId,
        stackId = event.stackId,
        requestId = event.requestId,
        logicalResourceId = event.logicalResourceId,
        data = Json.encodeToString(JsonObject.serializer(), data.toJsonObject())
    )
}
