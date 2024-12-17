/**
 * Copyright 2016 Bartosz Schiller
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
package com.github.barteksc.pdfviewer

import android.graphics.PointF
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ScaleGestureDetector.OnScaleGestureListener
import android.view.View
import android.view.View.OnTouchListener
import com.github.barteksc.pdfviewer.util.Constants.Pinch

/**
 * This Manager takes care of moving the PDFView,
 * set its zoom track user actions.
 */
internal class DragPinchManager(
    private val pdfView: PDFView,
    private val animationManager: AnimationManager
) : GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener, OnScaleGestureListener,
    OnTouchListener {
    private val gestureDetector: GestureDetector
    private val scaleGestureDetector: ScaleGestureDetector
    private var scrolling = false
    private var scaling = false
    private var enabledScrollingAndZooming = true

    init {
        gestureDetector = GestureDetector(pdfView.context, this)
        scaleGestureDetector = ScaleGestureDetector(pdfView.context, this)
        pdfView.setOnTouchListener(this)
    }

    fun enableScrollingAndZooming() {
        enabledScrollingAndZooming = true
    }

    fun disableScrollingAndZooming() {
        enabledScrollingAndZooming = false
    }

    fun disableLongpress() {
        gestureDetector.setIsLongpressEnabled(false)
    }

    override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            val onTapHandled = pdfView.callbacks.callOnTap(e)
            if (!onTapHandled) {
                val ps = pdfView.scrollHandle
                if (ps != null && !pdfView.documentFitsView()) {
                    if (!ps.shown()) {
                        ps.show()
                    } else {
                        ps.hide()
                    }
                }
            }
            pdfView.performClick()
            return true
    }

    private fun startPageFling(
        downEvent: MotionEvent,
        ev: MotionEvent,
        velocityX: Float,
        velocityY: Float
    ) {
        if (!checkDoPageFling(velocityX, velocityY)) {
            return
        }
        val direction: Int
        direction = if (pdfView.isSwipeVertical) {
            if (velocityY > 0) -1 else 1
        } else {
            if (velocityX > 0) -1 else 1
        }
        // get the focused page during the down event to ensure only a single page is changed
        val delta = if (pdfView.isSwipeVertical) ev.y - downEvent.y else ev.x - downEvent.x
        val offsetX = pdfView.currentXOffset - delta * pdfView.zoom
        val offsetY = pdfView.currentYOffset - delta * pdfView.zoom
        val startingPage = pdfView.findFocusPage(offsetX, offsetY)
        val targetPage = Math.max(0, Math.min(pdfView.pageCount - 1, startingPage + direction))
        val edge = pdfView.findSnapEdge(targetPage)
        val offset = pdfView.snapOffsetForPage(targetPage, edge)
        animationManager.startPageFlingAnimation(-offset)
    }

    override fun onDoubleTap(e: MotionEvent): Boolean {
        if (!pdfView.isDoubletapEnabled) {
            return false
        }
        if (pdfView.zoom <= pdfView.maxZoom - pdfView.zoomStep) {
            pdfView.zoomWithAnimation(e.x, e.y, pdfView.zoom * pdfView.zoomStep)
        } else {
            pdfView.resetZoomWithAnimation()
        }
        return true
    }

    override fun onDoubleTapEvent(e: MotionEvent): Boolean {
        return false
    }

    override fun onDown(e: MotionEvent): Boolean {
        animationManager.stopFling()
        return true
    }

    override fun onShowPress(e: MotionEvent) {}
    override fun onSingleTapUp(e: MotionEvent): Boolean {
        return false
    }

       override fun onScroll(
        e1: MotionEvent?,
        e2: MotionEvent,
        distanceX: Float,
        distanceY: Float
    ): Boolean {
        scrolling = true
        if (pdfView.isZooming || pdfView.isSwipeEnabled) {
            pdfView.moveRelativeTo(-distanceX, -distanceY)
        }
        if (!scaling || pdfView.doRenderDuringScale()) {
            pdfView.loadPageByOffset()
        }
        return true
    }

    private fun onScrollEnd(event: MotionEvent) {
        pdfView.loadPages()
        hideHandle()
        if (!animationManager.isFlinging) {
            pdfView.performPageSnap()
        }
    }

    override fun onLongPress(e: MotionEvent) {
        pdfView.callbacks.callOnLongPress(e)
    }

    override fun onFling(
        e1: MotionEvent?,
        e2: MotionEvent,
        velocityX: Float,
        velocityY: Float
    ): Boolean {
        if(e1 == null) return true
        if (!pdfView.isSwipeEnabled) {
            return false
        }
        if (true) {
            if (pdfView.pageFillsScreen()) {
                onBoundedFling(velocityX, velocityY)
            } else {
                startPageFling(e1, e2, velocityX, velocityY)
            }
            return true
        }
        val xOffset = pdfView.currentXOffset.toInt()
        val yOffset = pdfView.currentYOffset.toInt()
        val minX: Float
        val minY: Float
        val pdfFile = pdfView.pdfFile
        if (pdfView.isSwipeVertical) {
            minX = -(pdfView.toCurrentScale(pdfFile.maxPageWidth) - pdfView.width)
            minY = -(pdfFile.getDocLen(pdfView.zoom) - pdfView.height)
        } else {
            minX = -(pdfFile.getDocLen(pdfView.zoom) - pdfView.width)
            minY = -(pdfView.toCurrentScale(pdfFile.maxPageHeight) - pdfView.height)
        }
        animationManager.startFlingAnimation(
            xOffset,
            yOffset,
            velocityX.toInt(),
            velocityY.toInt(),
            minX.toInt(),
            0,
            minY.toInt(),
            0
        )
        return true
    }

    private fun onBoundedFling(velocityX: Float, velocityY: Float) {
        val xOffset = pdfView.currentXOffset.toInt()
        val yOffset = pdfView.currentYOffset.toInt()
        val pdfFile = pdfView.pdfFile
        val pageStart = -pdfFile.getPageOffset(pdfView.currentPage, pdfView.zoom)
        val pageEnd = pageStart - pdfFile.getPageLength(pdfView.currentPage, pdfView.zoom)
        val minX: Float
        val minY: Float
        val maxX: Float
        val maxY: Float
        if (pdfView.isSwipeVertical) {
            minX = -(pdfView.toCurrentScale(pdfFile.maxPageWidth) - pdfView.width)
            minY = pageEnd + pdfView.height
            maxX = 0f
            maxY = pageStart
        } else {
            minX = pageEnd + pdfView.width
            minY = -(pdfView.toCurrentScale(pdfFile.maxPageHeight) - pdfView.height)
            maxX = pageStart
            maxY = 0f
        }
        animationManager.startFlingAnimation(
            xOffset,
            yOffset,
            velocityX.toInt(),
            velocityY.toInt(),
            minX.toInt(),
            maxX.toInt(),
            minY.toInt(),
            maxY.toInt()
        )
    }

    override fun onScale(detector: ScaleGestureDetector): Boolean {
        var dr = detector.scaleFactor
        val wantedZoom = pdfView.zoom * dr
        val minZoom = Math.min(Pinch.MINIMUM_ZOOM, pdfView.minZoom)
        val maxZoom = Math.min(Pinch.MAXIMUM_ZOOM, pdfView.maxZoom)
        if (wantedZoom < minZoom) {
            dr = minZoom / pdfView.zoom
        } else if (wantedZoom > maxZoom) {
            dr = maxZoom / pdfView.zoom
        }
        pdfView.zoomCenteredRelativeTo(dr, PointF(detector.focusX, detector.focusY))
        return true
    }

    override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
        scaling = true
        return true
    }

    override fun onScaleEnd(detector: ScaleGestureDetector) {
        pdfView.loadPages()
        hideHandle()
        scaling = false
    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        if (!enabledScrollingAndZooming) {
            return false
        }
        var retVal = scaleGestureDetector.onTouchEvent(event)
        retVal = gestureDetector.onTouchEvent(event) || retVal
        if (event.action == MotionEvent.ACTION_UP) {
            if (scrolling) {
                scrolling = false
                onScrollEnd(event)
            }
        }
        return retVal
    }

    private fun hideHandle() {
        val scrollHandle = pdfView.scrollHandle
        if (scrollHandle != null && scrollHandle.shown()) {
            scrollHandle.hideDelayed()
        }
    }

    /**
     * Checks whether a page fling should be initiated, based on the swipe direction and velocities.
     * Fling is initiated if the direction is vertical
     */
    private fun checkDoPageFling(velocityX: Float, velocityY: Float): Boolean {
        val absX = Math.abs(velocityX)
        val absY = Math.abs(velocityY)
        return if (pdfView.isSwipeVertical) absY > absX else absX > absY
    }
}
