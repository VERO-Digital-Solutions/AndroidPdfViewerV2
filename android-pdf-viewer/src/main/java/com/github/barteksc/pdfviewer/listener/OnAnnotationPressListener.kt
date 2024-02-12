package com.github.barteksc.pdfviewer.listener

fun interface OnAnnotationPressListener {
    fun openDocumentation(schemaId: Long, documentId: String?)
}
