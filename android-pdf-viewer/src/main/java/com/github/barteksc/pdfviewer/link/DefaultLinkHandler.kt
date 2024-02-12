package com.github.barteksc.pdfviewer.link

/**
 * Copyright 2017 Bartosz Schiller
 *
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import android.util.Log
import com.github.barteksc.pdfviewer.PDFView
import com.github.barteksc.pdfviewer.listener.OnAnnotationPressListener
import com.github.barteksc.pdfviewer.annotation.core.pdf.PdfUtil
import com.github.barteksc.pdfviewer.annotation.core.shapes.checkIfPointIsInsideRect
import com.github.barteksc.pdfviewer.model.LinkTapEvent

class DefaultLinkHandler(
    private val pdfView: PDFView,
    private val pdfFilePath: String,
    private val listener: OnAnnotationPressListener
) :
    LinkHandler {
    override fun handleLinkEvent(event: LinkTapEvent) {
        Log.i(
            TAG,
            "handleLinkEvent - event ¬--> X: " + event.originalX + " | Y: " + event.originalY
        )

        val pdfPoint = pdfView.convertScreenPintsToPdfCoordinates(event.originalX, event.originalY)
        Log.i(TAG, "handleLinkEvent - pdfPoint --> X: " + pdfPoint.x + " | Y: " + pdfPoint.y)

        val extractedAnnotations = PdfUtil.getAnnotationsFrom(pdfFilePath, pageNum = 1)
        extractedAnnotations.forEach { annotation ->
            val isAnnotationMatchFound = checkIfPointIsInsideRect(
                pdfPoint,
                topRight = annotation.points[1],
                bottomLeft = annotation.points[3]
            )
            val documentation = annotation.relations?.documentation?.get(0)
            if (isAnnotationMatchFound) {
                if (documentation != null) {
                    listener.openDocumentation(
                        annotation.relations.documentation[0].schemaId,
                        annotation.relations.documentation[0].documentId
                    )
                    return@forEach
                }
            }
        }
    }

    companion object {
        private val TAG = DefaultLinkHandler::class.java.simpleName
    }
}