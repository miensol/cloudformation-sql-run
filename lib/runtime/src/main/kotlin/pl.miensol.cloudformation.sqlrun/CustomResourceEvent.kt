package pl.miensol.cloudformation.sqlrun

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

@Serializable(with = CustomResourceEventSerializer::class)
sealed class CustomResourceEvent {
    abstract val serviceToken: String
    abstract val responseURL: String
    abstract val stackId: String
    abstract val requestId: String
    abstract val logicalResourceId: String
    abstract val resourceType: String
    abstract val resourceProperties: CfnSqlRunProps
    abstract val requestType: String

    @Serializable
    @SerialName("Create")
    data class Create(
        @SerialName("ServiceToken") override val serviceToken: String,
        @SerialName("ResponseURL") override val responseURL: String,
        @SerialName("StackId") override val stackId: String,
        @SerialName("RequestId") override val requestId: String,
        @SerialName("LogicalResourceId") override val logicalResourceId: String,
        @SerialName("ResourceType") override val resourceType: String,
        @SerialName("ResourceProperties") override val resourceProperties: CfnSqlRunProps,
        @SerialName("RequestType") override val requestType: String
    ) : CustomResourceEvent()

    @Serializable
    @SerialName("Update")
    data class Update(
        @SerialName("ServiceToken") override val serviceToken: String,
        @SerialName("ResponseURL") override val responseURL: String,
        @SerialName("StackId") override val stackId: String,
        @SerialName("RequestId") override val requestId: String,
        @SerialName("LogicalResourceId") override val logicalResourceId: String,
        @SerialName("ResourceType") override val resourceType: String,
        @SerialName("ResourceProperties") override val resourceProperties: CfnSqlRunProps,
        @SerialName("PhysicalResourceId") val physicalResourceId: String,
        @SerialName("OldResourceProperties") val oldResourceProperties: CfnSqlRunProps,
        @SerialName("RequestType") override val requestType: String
    ) : CustomResourceEvent()

    @Serializable
    @SerialName("Delete")
    data class Delete(
        @SerialName("ServiceToken") override val serviceToken: String,
        @SerialName("ResponseURL") override val responseURL: String,
        @SerialName("StackId") override val stackId: String,
        @SerialName("RequestId") override val requestId: String,
        @SerialName("LogicalResourceId") override val logicalResourceId: String,
        @SerialName("ResourceType") override val resourceType: String,
        @SerialName("ResourceProperties") override val resourceProperties: CfnSqlRunProps,
        @SerialName("PhysicalResourceId") val physicalResourceId: String,
        @SerialName("RequestType") override val requestType: String,
    ) : CustomResourceEvent()
}

internal object CustomResourceEventSerializer :
    JsonContentPolymorphicSerializer<CustomResourceEvent>(CustomResourceEvent::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<out CustomResourceEvent> {
        return when (val rawRequestType = element.jsonObject["RequestType"]?.jsonPrimitive) {
            JsonPrimitive("Create") -> CustomResourceEvent.Create.serializer()
            JsonPrimitive("Update") -> CustomResourceEvent.Update.serializer()
            JsonPrimitive("Delete") -> CustomResourceEvent.Delete.serializer()
            else -> throw IllegalArgumentException("Unsupported RequestType=$rawRequestType")
        }
    }
}