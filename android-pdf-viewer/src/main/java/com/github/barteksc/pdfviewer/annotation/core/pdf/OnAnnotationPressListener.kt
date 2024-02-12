package com.github.barteksc.pdfviewer.annotation.core.pdf

fun interface OnAnnotationPressListener {
    fun openDocumentation(schemaId: Long, documentId: String?)
}
