package com.github.barteksc.sample

import android.graphics.PointF
import com.github.barteksc.pdfviewer.annotation.core.shapes.Documentation
import com.github.barteksc.pdfviewer.annotation.core.shapes.Rectangle
import com.github.barteksc.pdfviewer.annotation.core.shapes.Rectangle.Companion.generateRectangleEdges
import com.github.barteksc.pdfviewer.annotation.core.shapes.Relations

class TestDataHelper {
    companion object {
        fun getMockedRectangleCorners() = listOf(
            PointF(30.606155F, 22.65048F),
            PointF(60.60616F, 22.65048F),
            PointF(60.60616F, 62.65048F),
            PointF(30.606155F, 62.65048F)
        )

        fun getMockedRectangleRelations(): Relations {
            val documentation = Documentation(16, "583")
            val documentations = listOf(documentation)
            return Relations(documentations)
        }

        fun getMockedRectangle(): List<Rectangle> {
            val corners = getMockedRectangleCorners()
            val edges = generateRectangleEdges(corners)
            val relations = getMockedRectangleRelations()
            val rectangle = Rectangle(points = corners, edges = edges, relations = relations)
            return listOf(rectangle)
        }
    }
}