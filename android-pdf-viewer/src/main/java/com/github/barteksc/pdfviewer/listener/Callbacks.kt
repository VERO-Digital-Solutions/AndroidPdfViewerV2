package com.github.barteksc.pdfviewer.listener

import android.view.MotionEvent

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

import com.github.barteksc.pdfviewer.link.TapHandler
import com.github.barteksc.pdfviewer.model.LinkTapEvent

class Callbacks {
    /**
     * Call back object to call when the PDF is loaded
     */
    var onLoadCompleteListener: OnLoadCompleteListener? = null

    /**
     * Call back object to call when document loading error occurs
     */
    var onErrorListener: OnErrorListener? = null

    /**
     * Call back object to call when the page load error occurs
     */
    var onPageErrorListener: OnPageErrorListener? = null

    /**
     * Call back object to call when the document is initially rendered
     */
    var onRenderListener: OnRenderListener? = null

    /**
     * Call back object to call when the page has changed
     */
    var onPageChangeListener: OnPageChangeListener? = null

    /**
     * Call back object to call when the page is scrolled
     */
    var onPageScrollListener: OnPageScrollListener? = null

    /**
     * Call back object to call when the above layer is to drawn
     */
    var onDrawListener: OnDrawListener? = null

    var onDrawAllListener: OnDrawListener? = null

    /**
     * Call back object to call when the user does a tap gesture
     */
    var onTapListener: OnTapListener? = null

    /**
     * Call back object to call when the user does a long tap gesture
     */
    var onLongPressListener: OnLongPressListener? = null

    /**
     * Call back object to call when clicking link
     */
    var tapHandler: TapHandler? = null

    fun callOnLoadComplete(pagesCount: Int) {
        if (onLoadCompleteListener != null) {
            onLoadCompleteListener!!.loadComplete(pagesCount)
        }
    }

    fun callOnPageError(page: Int, error: Throwable?): Boolean {
        if (onPageErrorListener != null) {
            onPageErrorListener!!.onPageError(page, error)
            return true
        }
        return false
    }

    fun callOnRender(pagesCount: Int) {
        if (onRenderListener != null) {
            onRenderListener!!.onInitiallyRendered(pagesCount, 1F, 1F)
        }
    }

    fun callOnPageChange(page: Int, pagesCount: Int) {
        if (onPageChangeListener != null) {
            onPageChangeListener!!.onPageChanged(page, pagesCount)
        }
    }

    fun callOnPageScroll(currentPage: Int, offset: Float) {
        if (onPageScrollListener != null) {
            onPageScrollListener!!.onPageScrolled(currentPage, offset)
        }
    }

    fun callOnTap(event: MotionEvent?): Boolean {
        return onTapListener != null && onTapListener!!.onTap(event)
    }

    fun callOnLongPress(event: MotionEvent?) {
        onLongPressListener?.onLongPress(event)
    }

    fun callLinkHandler(event: LinkTapEvent?) {
        tapHandler?.handleLinkEvent(event)
    }
}