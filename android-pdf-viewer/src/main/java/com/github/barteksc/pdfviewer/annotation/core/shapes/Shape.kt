package com.github.barteksc.pdfviewer.annotation.core.shapes

import android.graphics.PointF
import com.github.barteksc.pdfviewer.annotation.core.annotations.Annotation
import com.github.barteksc.pdfviewer.annotation.core.annotations.SquareAnnotation
import com.github.barteksc.pdfviewer.annotation.core.pdf.PdfUtil
import com.github.barteksc.pdfviewer.util.logError
import com.github.salomonbrys.kotson.jsonNull
import com.github.salomonbrys.kotson.jsonObject
import com.github.salomonbrys.kotson.toJsonArray
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken

open class Shape(
    @Transient open val type: String = "",
    @Transient open val points: List<PointF> = emptyList(),
    open val relations: Relations? = null,
    open val edges: List<Edge> = emptyList(),
    open val colorHex: String = "",
) {
    companion object {
        val TAG: String = PdfUtil.javaClass.simpleName
    }

    fun toAnnotationOrNull(pageHeight: Int): Annotation? {
        fun toSquareAnnotation(pageHeight: Int): Annotation {
            val points = points.map { it.convertCoordinatesFrom(pageHeight) }
            return SquareAnnotation(
                points = points,
                relations = relations,
                colorHex = colorHex,
            )
        }

        return when (this.type) {
            ShapeType.RECTANGLE.name -> toSquareAnnotation(pageHeight)
            else -> {
                logError(TAG, "Couldn't parse shape with type $type to annotation")
                null
            }
        }
    }

    fun toJson(gson: Gson) = jsonObject(
        "type" to type,
        "points" to points.map { point ->
            jsonObject(
                "x" to point.x.toString(),
                "y" to point.y.toString(),
                "z" to jsonNull
            )
        }.toJsonArray(),
        "edges" to edges.map {
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
            )
        }.toJsonArray(),
        "relations" to gson.toJsonTree(relations),
        "color" to colorHex,
    )
}

fun fromJson(jsonShapes: String): List<Shape> {
    val listType = object : TypeToken<List<Shape>>() {}.type
    val gson =
        Gson().newBuilder().registerTypeAdapter(Shape::class.java, ShapeTypeAdapter())
            .create()
    return gson.fromJson(jsonShapes, listType)
}

fun List<Shape>.toJson(): String {
    val gson = GsonBuilder()
        .setLenient()
        .create()
    val shapesArray = this.map { shape ->
        shape.toJson(gson)
    }.toJsonArray()
    return shapesArray.toString()
}