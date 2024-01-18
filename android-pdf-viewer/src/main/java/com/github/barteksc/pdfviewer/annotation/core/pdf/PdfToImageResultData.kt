package com.github.barteksc.pdfviewer.annotation.core.pdf

import java.io.File

/** An instance of this class is created when we annotate the PDF.
 * @param pdfFile - the pdf file we want to annotate
 * @param imageFile - the PNG image that is created from the given PDF file's first page,
 * this image will be passed to MeasureLib
 * @param pageHeight - the height of the PDF page/image, needed when converting between PDF and image space
 * @param jsonShapes - the PDF annotations mapped to json shapes, that will be passed to MeasureLib */
data class PdfToImageResultData(
    val pdfFile: File,
    val imageFile: File,
    val pageHeight: Int,
    val jsonShapes: String
)