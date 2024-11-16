package com.github.barteksc.pdfviewer.annotation.core.shapes

import android.graphics.PointF
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

data class Rectangle(
    override val type: String = ShapeType.RECTANGLE.name,
    override val points: List<PointF> = emptyList(),
    override val edges: List<Edge> = emptyList(),
    override val relations: Relations? = null,
    override val colorHex: String
    ) : Shape(type, points) {
    companion object {
        fun generateRectangleEdges(points: List<PointF>): List<Edge> {
            val edgeTopHorizontal = Edge(points[0], points[1])
            val edgeRightVertical = Edge(points[1], points[2])
            val edgeBottomHorizontal = Edge(points[2], points[3])
            val edgeLeftVertical = Edge(points[3], points[0])
            return listOf(
                edgeTopHorizontal,
                edgeRightVertical,
                edgeBottomHorizontal,
                edgeLeftVertical
            )
        }
    }
}

data class Edge(val start: PointF, val end: PointF)

class ShapeTypeAdapter : JsonSerializer<Shape>, JsonDeserializer<Shape> {
    override fun serialize(
        src: Shape?,
        typeOfSrc: Type?,
        context: JsonSerializationContext?
    ): JsonElement {
        val jsonObject = JsonObject()
        jsonObject.addProperty("type", src?.type)
        jsonObject.add(
            "points",
            context?.serialize(src?.points, object : TypeToken<List<PointF>>() {}.type)
        )
        jsonObject.add(
            "edges",
            context?.serialize(src?.edges, object : TypeToken<List<Edge>>() {}.type)
        )
        jsonObject.add(
            "relations",
            context?.serialize(src?.relations, object : TypeToken<Relations>() {}.type)
        )
        jsonObject.addProperty("color", src?.colorHex)
        return jsonObject
    }

    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): Shape {
        val jsonObject = json?.asJsonObject
        val type = jsonObject?.get("type")?.asString ?: ""
        val corners = context?.deserialize<List<PointF>>(
            jsonObject?.get("points"),
            object : TypeToken<List<PointF>>() {}.type
        ) ?: emptyList()
        val edges = context?.deserialize<List<Edge>>(
            jsonObject?.get("edges"),
            object : TypeToken<List<Edge>>() {}.type
        ) ?: emptyList()
        val relations = context?.deserialize<Relations>(
            jsonObject?.get("relations"),
            object : TypeToken<Relations>() {}.type
        )
        val colorHex = jsonObject?.get("color")?.asString ?: ""
        return Shape(type, corners, relations, edges, colorHex)
    }
}