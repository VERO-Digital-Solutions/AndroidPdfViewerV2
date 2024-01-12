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
import com.github.barteksc.pdfviewer.model.LinkTapEvent

class DefaultLinkHandler(private val pdfView: PDFView) : LinkHandler {
    override fun handleLinkEvent(event: LinkTapEvent) {
        Log.i(TAG, "handleLinkEvent - event ¬--> X: " + event.originalX + " | Y: " + event.originalY)
        Log.i(TAG, "--------------------------------------------------")

        val pdfPoint = pdfView.convertScreenPintsToPdfCoordinates(event.originalX, event.originalY)
        Log.i(TAG, "handleLinkEvent - pdfPoint --> X: " + pdfPoint.x + " | Y: " + pdfPoint.y)
        Log.i(TAG, "--------------------------------------------------")
    }

    companion object {
        private val TAG = DefaultLinkHandler::class.java.simpleName
    }
}