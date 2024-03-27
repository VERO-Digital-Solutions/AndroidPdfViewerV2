package com.github.barteksc.pdfviewer.annotation.core.annotations

import android.graphics.PointF
import com.github.barteksc.pdfviewer.annotation.core.shapes.Rectangle
import com.github.barteksc.pdfviewer.annotation.core.shapes.Rectangle.Companion.generateRectangleEdges
import com.github.barteksc.pdfviewer.annotation.core.shapes.Relations
import com.github.barteksc.pdfviewer.annotation.core.shapes.convertCoordinatesFrom

/**  Points are in the order: topLeft, topRight, bottomRight, bottomLeft */
open class Annotation(
    val type: String,
    open val points: List<PointF>,
)

class SquareAnnotation(
    points: List<PointF>,
    val relations: Relations? = null,
) : Annotation(type = AnnotationType.SQUARE.name, points) {
    fun toRectangleShape(pageHeight: Int): Rectangle {
        // rectangle's corners  mapped to image space
        val mappedPoints = listOf(
            points[0].convertCoordinatesFrom(pageHeight),
            points[1].convertCoordinatesFrom(pageHeight),
            points[2].convertCoordinatesFrom(pageHeight),
            points[3].convertCoordinatesFrom(pageHeight)
        )

        // rectangle shape's edges
        val rectangleShapeEdges = generateRectangleEdges(mappedPoints)

        return Rectangle(points = mappedPoints, edges = rectangleShapeEdges, relations = relations)
    }
}

class LinkAnnotation(points: List<PointF>, val uri: String?) :
    Annotation(type = AnnotationType.LINK.name, points)
