package com.agentickitchen.android

import android.content.Context
import com.agentickitchen.shared.models.HardwareProfile
import java.io.File

class HardwareProfileManager(private val context: Context) {
    fun save(profile: HardwareProfile) {
        val file = File(context.filesDir, "hardware_profile_${profile.id}.json")
        file.writeText(toJson(profile))
    }

    fun load(id: String): HardwareProfile? {
        val file = File(context.filesDir, "hardware_profile_$id.json")
        if (!file.exists()) return null
        val text = file.readText()
        // very simple parse - for full fidelity use kotlinx.serialization
        // attempt naive parsing of fields
        val idVal = extractString(text, "id") ?: return null
        val typeVal = extractString(text, "type") ?: "stovetop"
        val fuelVal = extractString(text, "fuel") ?: "electric"
        val min = extractInt(text, "controlRangeMin") ?: 1
        val max = extractInt(text, "controlRangeMax") ?: 10
        // skipping heatMap parsing for simplicity
        return HardwareProfile(idVal, typeVal, fuelVal, min, max, emptyMap(), emptyMap())
    }

    private fun toJson(profile: HardwareProfile): String {
        val heatMapJson = mapToJson(profile.heatMap)
        val preheatJson = mapToJson(profile.preheatTimeSecForTemp)
        return "{" +
                "\"id\":\"${escape(profile.id)}\"," +
                "\"type\":\"${escape(profile.type)}\"," +
                "\"fuel\":\"${escape(profile.fuel)}\"," +
                "\"controlRangeMin\":${profile.controlRangeMin}," +
                "\"controlRangeMax\":${profile.controlRangeMax}," +
                "\"heatMap\":$heatMapJson," +
                "\"preheatTimeSecForTemp\":$preheatJson" +
                "}"
    }

    private fun mapToJson(map: Map<Int, Int>): String {
        if (map.isEmpty()) return "{}"
        return map.entries.joinToString(prefix = "{", postfix = "}") { "\"${it.key}\":${it.value}" }
    }

    private fun extractString(json: String, key: String): String? {
        val regex = """"$key"\s*:\s*"(.*?)"""".toRegex()
        return regex.find(json)?.groups?.get(1)?.value
    }

    private fun extractInt(json: String, key: String): Int? {
        val regex = """"$key"\s*:\s*(\d+)"""".toRegex()
        return regex.find(json)?.groups?.get(1)?.value?.toIntOrNull()
    }

    private fun escape(value: String): String = value.replace("\"", "\\\"")
}
