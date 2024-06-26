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
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.MotionEvent
import androidx.core.net.toFile
import com.github.barteksc.pdfviewer.PDFView
import com.github.barteksc.pdfviewer.annotation.core.annotations.LinkAnnotation
import com.github.barteksc.pdfviewer.annotation.core.annotations.SquareAnnotation
import com.github.barteksc.pdfviewer.annotation.core.pdf.PdfUtil
import com.github.barteksc.pdfviewer.annotation.core.shapes.checkIfPointIsInsideAnnotation
import com.github.barteksc.pdfviewer.listener.OnAnnotationPressListener
import com.github.barteksc.pdfviewer.listener.OnTapListener
import com.github.barteksc.pdfviewer.util.logInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class CustomOnTapListener(
    private val pdfView: PDFView,
    private val pdfUri: Uri,
    private val handleAnnotations: Boolean,
    private val listener: OnAnnotationPressListener
) : OnTapListener {

    override fun onTap(e: MotionEvent): Boolean {
        return try {
            val pdfFilePath = pdfUri.toFile().absolutePath
            Log.i(TAG, "tap event --> X: " + e.x + " | Y: " + e.y)
            val pdfPoint = pdfView.convertScreenPintsToPdfCoordinates(e.x, e.y)
            Log.i(TAG, "pdfPoint --> X: " + pdfPoint.x + " | Y: " + pdfPoint.y)
            val extractedAnnotations = runBlocking(Dispatchers.IO) {
                PdfUtil.getAnnotationsFrom(pdfFilePath, pageNum = 1)
            }
            val clickedAnnotation = extractedAnnotations.firstOrNull { annotation ->
                checkIfPointIsInsideAnnotation(pdfPoint, annotation)
            } ?: return false

            return when (clickedAnnotation) {
                is LinkAnnotation -> {
                    handleUri(clickedAnnotation.uri)
                    true
                }

                is SquareAnnotation -> {
                    if (handleAnnotations) {
                        if (clickedAnnotation.relations?.documentation?.isNotEmpty() == true) {
                            listener.onAnnotationPressed(clickedAnnotation.relations.documentation[0])
                        }
                    }
                    true
                }

                else -> false
            }
        } catch (e: IllegalArgumentException) {
            logInfo(TAG, "Extracting filepath for content uri is not possible")
            false
        }
    }

    companion object {
        private val TAG = CustomOnTapListener::class.java.simpleName
    }

    private fun handleUri(uri: String?) {
        logInfo(TAG, "URI for tapped link annotation is $uri")
        val webLink = uri ?: return
        if (!webLink.startsWith("http")) {
            Log.w(TAG, "URI $uri is not a web link")
            return
        }

        val parsedUri = Uri.parse(uri)
        val intent = Intent(Intent.ACTION_VIEW, parsedUri)
        val context: Context = pdfView.context
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            Log.w(TAG, "No activity found for URI: $webLink")
        }
    }
}