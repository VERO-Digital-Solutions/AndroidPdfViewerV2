package com.github.barteksc.pdfviewer.annotation.core.pdf

interface DocumentationOpener {
    fun openDocumentation(schemaId: Long, documentationID: String?)
}
