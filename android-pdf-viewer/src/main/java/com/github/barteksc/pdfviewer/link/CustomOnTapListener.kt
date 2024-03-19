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
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.MotionEvent
import androidx.annotation.RequiresApi
import androidx.core.net.toFile
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.github.barteksc.pdfviewer.PDFView
import com.github.barteksc.pdfviewer.annotation.core.pdf.PdfUtil
import com.github.barteksc.pdfviewer.annotation.core.shapes.checkIfPointIsInsideAnnotation
import com.github.barteksc.pdfviewer.listener.OnAnnotationPressListener
import com.github.barteksc.pdfviewer.listener.OnTapListener
import com.github.barteksc.pdfviewer.util.logInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.CompletableFuture

class CustomOnTapListener(
    private val pdfView: PDFView,
    private val pdfUri: Uri,
    private val context: Context,
    private val listener: OnAnnotationPressListener,
) : OnTapListener {

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onTap(e: MotionEvent?): Boolean {
        if (e == null) {
            logInfo(TAG, "Motion event is null")
            return false
        } else {
            val act = (context as? FragmentActivity)
            val resultFuture = CompletableFuture<Boolean>()

            act?.lifecycleScope?.launch {
                try {
                    withContext(Dispatchers.IO) {
                        val pdfFilePath = pdfUri.toFile().absolutePath
                        Log.i(TAG, "tap event --> X: " + e.x + " | Y: " + e.y)
                        val pdfPoint = pdfView.convertScreenPintsToPdfCoordinates(e.x, e.y)
                        Log.i(TAG, "pdfPoint --> X: " + pdfPoint.x + " | Y: " + pdfPoint.y)
                        val extractedAnnotations =
                            PdfUtil.getAnnotationsFrom(pdfFilePath, pageNum = 1)
                        val clickedAnnotation = extractedAnnotations.firstOrNull { annotation ->
                            checkIfPointIsInsideAnnotation(pdfPoint, annotation)
                        }
                        val annotationPressed = clickedAnnotation?.relations?.documentation?.getOrNull(0)
                        if (annotationPressed != null) {
                            listener.onAnnotationPressed(annotationPressed)
                            resultFuture.complete(true)
                        } else {
                            resultFuture.complete(false)
                        }
                    }
                } catch (e: IllegalArgumentException) {
                    logInfo(TAG, "Extracting filepath for content uri is not possible")
                    resultFuture.complete(false)
                }
            }

            return resultFuture.get()
        }
    }

    companion object {
        private val TAG = CustomOnTapListener::class.java.simpleName
    }
}