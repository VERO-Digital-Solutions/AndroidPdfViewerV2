package com.github.barteksc.pdfviewer.annotation.core.shapes

import android.graphics.PointF
import com.github.barteksc.pdfviewer.annotation.core.annotations.Annotation

/** Convert image coordinates to pdf coordinates and vice versa, no scale. Y-axis is inverted */
fun PointF.convertCoordinatesFrom(pageHeight: Int) = PointF(x, pageHeight - y)

/** From the given diagonal bottom left and top right points, calculate the other 2 points of
 *  the rectangle that is around the annotation. Rectangle in OpenPDF is constructed by 2 points
 *  [OpenPDF Rectangle](https://github.com/LibrePDF/OpenPDF/blob/master/openpdf/src/main/java/com/lowagie/text/Rectangle.java).
 * */
fun generateRectCoordinates(bottomLeft: PointF, topRight: PointF): List<PointF> {
    val topLeft = PointF(bottomLeft.x, topRight.y)
    val bottomRight = PointF(topRight.x, bottomLeft.y)

    return listOf(topLeft, topRight, bottomRight, bottomLeft)
}

/** Checks if a point is inside the given annotation's rectangle */
fun checkIfPointIsInsideAnnotation(point: PointF, annotation: Annotation): Boolean {
    val (topLeft, topRight, bottomRight, bottomLeft) = annotation.points

    return point.x >= bottomLeft.x && point.x <= bottomRight.x &&
            point.x >= topLeft.x && point.x <= topRight.x &&
            point.y >= bottomLeft.y && point.y <= topLeft.y &&
            point.y >= bottomRight.y && point.y <= topRight.y
}