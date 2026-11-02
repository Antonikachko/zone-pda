
package com.zonepda

import org.json.JSONArray
import org.json.JSONObject

data class MapMarkerData(
    val id: Long,
    val name: String,
    val category: String,
    val latitude: Double,
    val longitude: Double
)

object MapMarkerStorage {

    private const val PREFS = "zone_pda_data"
    private const val KEY = "map_markers"

    fun save(
        context: android.content.Context,
        markers: List<MapMarkerData>
    ) {
        val array = JSONArray()

        markers.forEach { marker ->
            val item = JSONObject().apply {
                put("id", marker.id)
                put("name", marker.name)
                put("category", marker.category)
                put("latitude", marker.latitude)
                put("longitude", marker.longitude)
            }
            array.put(item)
        }

        context.getSharedPreferences(
            PREFS,
            android.content.Context.MODE_PRIVATE
        ).edit()
            .putString(KEY, array.toString())
            .apply()
    }

    fun load(
        context: android.content.Context
    ): List<MapMarkerData> {
        val json = context.getSharedPreferences(
            PREFS,
            android.content.Context.MODE_PRIVATE
        ).getString(KEY, "[]") ?: "[]"

        return try {
            val array = JSONArray(json)
            val result = mutableListOf<MapMarkerData>()

            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)

                result.add(
                    MapMarkerData(
                        id = item.getLong("id"),
                        name = item.getString("name"),
                        category = item.getString("category"),
                        latitude = item.getDouble("latitude"),
                        longitude = item.getDouble("longitude")
                    )
                )
            }

            result
        } catch (e: Exception) {
            emptyList()
        }
    }
}
