package com.github.barteksc.pdfviewer.annotation.core.shapes

import android.graphics.PointF
import com.github.barteksc.pdfviewer.annotation.core.annotations.Annotation
import com.github.barteksc.pdfviewer.annotation.core.annotations.AnnotationType
import com.github.barteksc.pdfviewer.annotation.core.pdf.PdfUtil
import com.github.barteksc.pdfviewer.util.logError
import com.github.salomonbrys.kotson.toJsonArray
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

open class Shape(
    @Transient open val type: String = "",
    @Transient open val points: List<PointF> = emptyList()
) {
    companion object {
        val TAG: String = PdfUtil.javaClass.simpleName
    }
}

fun Shape.toAnnotationOrNull(pageHeight: Int): Annotation? {
    return when (this) {
        is Rectangle -> toRectangleAnnotation(pageHeight)
        else -> {
            logError(Shape.TAG, "Couldn't parse shape $this to annotation")
            null
        }
    }
}

fun Shape.toRectangleAnnotation(pageHeight: Int): Annotation {
    val points = points.map { it.convertCoordinatesFrom(pageHeight) }
    return Annotation(
        type = AnnotationType.SQUARE.name,
        points = points,
        relations = (this as Rectangle).relations
    )
}

class ShapeDeserializer : JsonDeserializer<Shape> {
    private val TAG: String = ShapeDeserializer::javaClass.name

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): Shape? {
        val jsonObject = json.asJsonObject
        val shapeType = jsonObject.get("type").asString

        return when (shapeType) {
            ShapeType.RECTANGLE.name -> context.deserialize(json, Rectangle::class.java)
            else -> {
                logError(TAG, "JSON Parse error: Unknown shape type: $shapeType")
                null
            }
        }
    }
}

fun fromJson(jsonShapes: String): List<Rectangle> {
    val listType = object : TypeToken<List<Rectangle>>() {}.type
    val gson =
        Gson().newBuilder().registerTypeAdapter(Rectangle::class.java, RectangleTypeAdapter())
            .create()
    return gson.fromJson(jsonShapes, listType)
}

fun toJson(rectangles: List<Rectangle>): String {
    val gson = GsonBuilder()
        .setLenient()
        .create()
    val shapesArray = rectangles.map { rectangle ->
        toJson(rectangle, gson)
    }.toJsonArray()
    return shapesArray.toString()
}