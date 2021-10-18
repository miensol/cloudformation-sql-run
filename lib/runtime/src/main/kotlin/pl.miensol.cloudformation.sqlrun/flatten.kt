package pl.miensol.cloudformation.sqlrun

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject

fun flattenOrEmpty(obj: JsonElement?): JsonElement {
    val result = mutableMapOf<String, JsonElement>()
    flatten(obj, result)
    return JsonObject(result)
}

private fun flatten(obj: JsonElement?, result: MutableMap<String, JsonElement>, path: MutableList<String> = mutableListOf()) {
    return when (obj) {
        is JsonObject -> obj.forEach { key, child ->
            path.add(key)
            flatten(child, result, path)
            path.removeLast()
        }
        is JsonArray -> obj.forEachIndexed { key, child ->
            path.add(key.toString())
            flatten(child, result, path)
            path.removeLast()
        }
        else -> {
            result[path.joinToString(separator = ".")] = obj ?: JsonNull
        }
    }
}