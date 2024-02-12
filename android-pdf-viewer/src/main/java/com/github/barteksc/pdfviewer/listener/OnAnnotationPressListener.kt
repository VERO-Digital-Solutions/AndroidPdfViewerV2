package com.github.barteksc.pdfviewer.listener

import com.github.barteksc.pdfviewer.annotation.core.shapes.Documentation

fun interface OnAnnotationPressListener {
//    fun openDocumentation( schemaId: Long, documentId: String?)
    fun openDocumentation( documentation: Documentation?)
}
