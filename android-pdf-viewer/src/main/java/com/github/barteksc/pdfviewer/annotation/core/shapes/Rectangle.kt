package com.github.barteksc.pdfviewer.annotation.core.shapes

import android.graphics.PointF
import com.github.salomonbrys.kotson.jsonNull
import com.github.salomonbrys.kotson.jsonObject
import com.github.salomonbrys.kotson.toJsonArray
import com.google.gson.Gson
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
    val edges: List<Edge> = emptyList(),
    val relations: Relations? = null,
) : Shape(type, points){
    companion object{
        fun generateRectangleEdges(points:List<PointF>): List<Edge> {
            val edgeTopHorizontal = Edge(points[0], points[1])
            val edgeRightVertical = Edge(points[1], points[2])
            val edgeBottomHorizontal = Edge(points[2], points[3])
            val edgeLeftVertical = Edge(points[3], points[0])
            return listOf(edgeTopHorizontal, edgeRightVertical, edgeBottomHorizontal, edgeLeftVertical)
        }
    }
}

fun toJson(rectangle: Rectangle, gson: Gson) = jsonObject(
    "type" to rectangle.type,
    "points" to rectangle.points.map { point ->
        jsonObject(
            "x" to point.x.toString(),
            "y" to point.y.toString(),
            "z" to jsonNull
        )
    }.toJsonArray(),
    "edges" to rectangle.edges.map {
        jsonObject(
            "start" to jsonObject(
                "x" to it.start.x.toString(),
                "y" to it.start.y.toString(),
                "z" to jsonNull
            ),
            "end" to jsonObject(
                "x" to it.end.x.toString(),
                "y" to it.end.y.toString(),
                "z" to jsonNull
            ),
            "value" to "0.0",
            "unit" to "",
            "distance" to jsonNull,
            "name" to "",
            "colorCode" to ""
        )
    }.toJsonArray(),
    "relations" to gson.toJsonTree(rectangle.relations)
)

data class Edge(val start: PointF, val end: PointF)

class RectangleTypeAdapter : JsonSerializer<Rectangle>, JsonDeserializer<Rectangle> {
    override fun serialize(
        src: Rectangle?,
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
        return jsonObject
    }

    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): Rectangle {
        val jsonObject = json?.asJsonObject
        val type = jsonObject?.get("type")?.asString ?: ShapeType.RECTANGLE.name
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
        return Rectangle(type, corners, edges, relations)
    }
}