package com.github.barteksc.pdfviewer.annotation.core.annotations

import android.graphics.PointF
import com.github.barteksc.pdfviewer.annotation.core.shapes.Relations
import com.github.barteksc.pdfviewer.util.Constants
import com.github.barteksc.pdfviewer.util.logError
import com.lowagie.text.Rectangle
import com.lowagie.text.pdf.PdfAnnotation
import com.lowagie.text.pdf.PdfArray
import com.lowagie.text.pdf.PdfDictionary
import com.lowagie.text.pdf.PdfLayer
import com.lowagie.text.pdf.PdfName
import com.lowagie.text.pdf.PdfNumber
import com.lowagie.text.pdf.PdfReader
import com.lowagie.text.pdf.PdfStamper
import com.lowagie.text.pdf.PdfString
import java.awt.Color
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

object AnnotationManager {
    private val TAG = AnnotationManager.javaClass.simpleName

    /** Draws a layer with a rectangle annotation to a PDF document with 1 page */
    @JvmStatic
    fun addRectangleAnnotation(
        rectCorners: List<PointF>,
        file: File,
        relations: Relations? = null,
        colorHex: String,
    ): Boolean {
        var isAdded = false
        try {
            if (!file.exists()) throw FileNotFoundException()
            val page = 1

            val referenceHash = StringBuilder()
                .append(Constants.KEY_REFERENCE_HASH)
                .append(UUID.randomUUID().toString())
                .toString()

            val inputStream: InputStream = FileInputStream(file)
            val reader = PdfReader(inputStream)
            val stamp = PdfStamper(reader, FileOutputStream(file))
            val annotationLayer = PdfLayer(referenceHash, stamp.writer)

            val lowerLeftX = rectCorners[3].x
            val lowerLeftY = rectCorners[3].y
            val upperRightX = rectCorners[1].x
            val upperRightY = rectCorners[1].y
            val rectAnnotation = PdfAnnotation.createSquareCircle(
                stamp.writer,
                Rectangle(
                    lowerLeftX,
                    lowerLeftY,
                    upperRightX,
                    upperRightY
                ),
                "",
                true
            )
            val relationsArray = PdfArray()
            relations?.documentation?.forEach {
                val documentationDict = PdfDictionary(PdfName("documentation"))
                documentationDict.put(PdfName("schemaId"), PdfNumber(it.schemaId))
                documentationDict.put(PdfName("documentId"), PdfString(it.documentId))
                relationsArray.add(documentationDict)
            }
            rectAnnotation.apply {
                val color: Color = convertFromColorHexToColor(colorHex)
                setColor(color)
                put(PdfName("relations"), relationsArray)
            }

            // add the annotation into target page
            val over = stamp.getOverContent(page)
            if (over == null) {
                stamp.close()
                reader.close()
                throw java.lang.Exception("GetUnderContent() is null")
            }
            // add the annotation to the layer
            over.beginLayer(annotationLayer)
            stamp.addAnnotation(rectAnnotation, page)
            over.endLayer()

            stamp.close()
            reader.close()
            isAdded = true
        } catch (e: Exception) {
            e.message?.let { logError(TAG, it) }
            e.printStackTrace()
        }
        return isAdded
    }

    /** Converts the color hex string received from MeasureLib to [Color] */
    private fun convertFromColorHexToColor(hexWithPrefix: String): Color {
        val hex = hexWithPrefix.removePrefix("#")
        val numberSystemBase = 16
        // AARRGGBB format's length
        val aarrggbbLength = 8
        return when (hex.length) {
            aarrggbbLength -> {
                /** Start and end indices for #AARRGGBB format
                Skip indices [0, 1] because [com.lowagie.text.pdf.PdfColor] doesn't store alpha */
                val redStartIndex = 2
                val redEndIndex = 4
                val greenStartIndex = 4
                val greenEndIndex = 6
                val blueStartIndex = 6
                val blueEndIndex = 8

                val red = hex.substring(redStartIndex, redEndIndex).toInt(numberSystemBase)
                val green = hex.substring(greenStartIndex, greenEndIndex).toInt(numberSystemBase)
                val blue = hex.substring(blueStartIndex, blueEndIndex).toInt(numberSystemBase)
                Color(red, green, blue)
            }
            else -> {
                throw IllegalArgumentException("Invalid color format. Couldn't parse the string $hexWithPrefix")
            }
        }
    }

    /** Removes all annotations from a given PDF file */
    @JvmStatic
    fun removeAnnotationsFromPdf(pdfFilePath: String): Boolean {
        if (pdfFilePath.isEmpty()) throw FileNotFoundException("Input file is empty")
        val file = File(pdfFilePath)
        if (!file.exists()) throw FileNotFoundException("Input file does not exist")
        return try {
            val inputStream: InputStream = FileInputStream(file)
            val pdfReader = PdfReader(inputStream)
            // the stamper will copy the document to a new file
            val pdfStamper = PdfStamper(pdfReader, FileOutputStream(file))
            pdfReader.removeAnnotations()

            // closing the stamper will generate the new PDF file
            pdfStamper.close()
            pdfReader.close()
            true
        } catch (e: Exception) {
            e.message?.let { logError(TAG, it) }
            e.printStackTrace()
            false
        }
    }
}