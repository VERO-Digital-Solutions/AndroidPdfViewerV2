package com.github.barteksc.pdfviewer.annotation.core.pdf

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PointF
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.github.barteksc.pdfviewer.annotation.core.annotations.Annotation
import com.github.barteksc.pdfviewer.annotation.core.annotations.AnnotationManager
import com.github.barteksc.pdfviewer.annotation.core.annotations.AnnotationType
import com.github.barteksc.pdfviewer.annotation.core.annotations.toRectangleShape
import com.github.barteksc.pdfviewer.annotation.core.shapes.Documentation
import com.github.barteksc.pdfviewer.annotation.core.shapes.Rectangle
import com.github.barteksc.pdfviewer.annotation.core.shapes.Relations
import com.github.barteksc.pdfviewer.annotation.core.shapes.Shape
import com.github.barteksc.pdfviewer.annotation.core.shapes.generateRectangleCoordinates
import com.github.barteksc.pdfviewer.annotation.core.shapes.mapJsonStringToPdfShapes
import com.github.barteksc.pdfviewer.annotation.core.shapes.mapPdfShapesToJsonString
import com.github.barteksc.pdfviewer.annotation.core.shapes.toAnnotation
import com.github.barteksc.pdfviewer.util.logDebug
import com.github.barteksc.pdfviewer.util.logError
import com.lowagie.text.pdf.PdfArray
import com.lowagie.text.pdf.PdfDictionary
import com.lowagie.text.pdf.PdfName
import com.lowagie.text.pdf.PdfNumber
import com.lowagie.text.pdf.PdfReader
import com.lowagie.text.pdf.PdfString
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import kotlin.properties.Delegates

object PdfUtil {

    private val TAG: String = PdfUtil.javaClass.simpleName

    /** Uses the passed PDF file to create a PNG image from the first page,
     *  maps the PDF annotations to shapes that will be saved as json string */
    @JvmStatic
    @Throws(IOException::class)
    fun getPdfToImageResultData(
        pdfFilePath: String,
        outputDirectory: String
    ): PdfToImageResultData {
        // (PDF, annotations) -> (PNG, shapes)
        val resultData = convertPdfAnnotationsToPngShapes(pdfFilePath, outputDirectory)
        // Remove annotations from the PDF so we can draw shapes from MeasureLib on the same PDF
        AnnotationManager.removeAnnotationsFromPdf(pdfFilePath)
        return resultData
    }

    /** Maps shapes to annotations and draws them to the given PDF */
    @JvmStatic
    fun drawAnnotations(pdfFile: File, pdfPageHeight: Int, jsonShapes: String) {
        val shapes = mapJsonStringToPdfShapes(jsonShapes)
        drawPngShapesToPdf(pdfFile, pdfPageHeight, shapes)
    }

    /** Extract the annotations for the given PDF file path and page number.
     * In OpenPdf, pages start from 1
     * Page number is always 1 for now */
    @JvmStatic
    fun getAnnotationsFrom(filePath: String, pageNum: Int): List<Annotation> {
        try {
            if (filePath.isEmpty()) throw Exception("Input file is empty")
            val file = File(filePath)
            if (!file.exists()) throw Exception("Input file does not exist")

            val inputStream: InputStream = FileInputStream(file)
            val reader = PdfReader(inputStream)
            val annotationsList = mutableListOf<Annotation>()

            // read annotations for the given page
            val page: PdfDictionary = reader.getPageN(pageNum)
            val annots: PdfArray? = page.getAsArray(PdfName.ANNOTS)
            if (annots == null) {
                logError(TAG, "Annotations array for page $pageNum is null")
            } else {
                logDebug(TAG, "Annotations array for page $pageNum: $annots")
                // Annotations that have a Rectangle (com/lowagie/text/Rectangle.java)
                val annotationsWithRect = listOf<PdfName>(PdfName.SQUARE)
                for (i in 0 until annots.size()) {
                    val annotation: PdfDictionary = annots.getAsDict(i)
                    // Extract extras
                    // coordinates of 2 corners of the rectangle of the annotation
                    val rectArray: PdfArray? = annotation.getAsArray(PdfName.RECT)
                    // type of annotation
                    val subtype: PdfName? = annotation.getAsName(PdfName.SUBTYPE)
                    if (subtype != null && subtype in annotationsWithRect) {
                        if (rectArray != null && rectArray.size() == 4) {
                            // bottom left corner's coordinates
                            val llx: Float = rectArray.getAsNumber(0).floatValue()
                            val lly: Float = rectArray.getAsNumber(1).floatValue()
                            // top right corner's coordinates
                            val urx: Float = rectArray.getAsNumber(2).floatValue()
                            val ury: Float = rectArray.getAsNumber(3).floatValue()

                            val extractedAnnotation: Annotation? = when (subtype) {
                                PdfName.SQUARE -> getExtractedSquareAnnotation(
                                    annotation,
                                    llx,
                                    lly,
                                    urx,
                                    ury,
                                )

                                else -> null
                            }
                            if (extractedAnnotation != null) {
                                annotationsList.add(extractedAnnotation)
                            }
                        }
                    } else {
                        logError(TAG, "Annotation is not recognised")
                    }
                }
            }
            return annotationsList
        } catch (e: Exception) {
            e.message?.let { logError(TAG, it) }
            e.printStackTrace()
            return emptyList()
        }
    }

    private fun getExtractedSquareAnnotation(
        annotation: PdfDictionary,
        llx: Float,
        lly: Float,
        urx: Float,
        ury: Float
    ): Annotation {
        val xBottomLeftPoint = llx
        val yBottomLeftPoint = lly
        val bottomLeftPoint = PointF(xBottomLeftPoint, yBottomLeftPoint)
        val xTopRightPoint = urx
        val yTopRightPoint = ury
        val topRightPoint = PointF(xTopRightPoint, yTopRightPoint)

        // from the extracted coordinates, calculate the rest
        val squareAnnotationPoints =
            generateRectangleCoordinates(bottomLeftPoint, topRightPoint)

        val relationsArray: PdfArray? =
            annotation.getAsArray(PdfName("relations"))

        return Annotation(
            AnnotationType.SQUARE.name,
            squareAnnotationPoints,
            relations = getExtractedRelations(relationsArray)
        )
    }

    private fun getExtractedRelations(relationsArray: PdfArray?): Relations? {
        val documentations = mutableListOf<Documentation>()
        if (relationsArray != null) {
            for (j in 0 until relationsArray.size()) {
                val documentationDict: PdfDictionary =
                    relationsArray.getAsDict(j)
                val schemaId: PdfNumber? =
                    documentationDict.getAsNumber(PdfName("schemaId"))
                val documentId: PdfString? =
                    documentationDict.getAsString(PdfName("documentId"))

                if (schemaId != null && documentId != null) {
                    documentations.add(
                        Documentation(
                            schemaId.intValue().toLong(),
                            documentId.toString()
                        )
                    )
                }
            }
        } else {
            return null
        }
        return Relations(documentations)
    }

    /** Map annotations to shapes,
     *  using the page height when converting between PDF space and image space */
    private fun getShapesFor(
        pdfAnnotations: List<Annotation>,
        pageHeight: Int
    ): List<Shape> {
        // convert annotation to shape
        val shapes = pdfAnnotations.map { annotation ->
            when (annotation.type) {
                AnnotationType.SQUARE.name -> return@map annotation.toRectangleShape(pageHeight)
                else -> throw Exception("Annotation is not recognised")
            }
        }
        return shapes
    }

    /** Use the passed PDF file path to map the PDF page to PNG image and
     *  map PDF annotations to image shapes, save the PNG image to the given output directory
     */
    @JvmStatic
    @Throws(Exception::class)
    private fun convertPdfAnnotationsToPngShapes(
        pdfPath: String, outputDirectory: String
    ): PdfToImageResultData {
        lateinit var pngFile: File
        var pageHeight by Delegates.notNull<Int>()
        lateinit var jsonShapes: String

        val fd = getSeekableFileDescriptor(pdfPath)
            ?: throw Exception("Couldn't get seek-able file descriptor")
        val renderer = PdfRenderer(fd)
        renderer.use { renderer ->
            // Assuming the pdf will have only 1 page (for now)
            val pageNum = 0
            val page = renderer.openPage(pageNum)
            pageHeight = page.height
            val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
            // Ensure white background
            bitmap.eraseColor(Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

            val pdfName = extractFileNameFromPath(pdfPath)
            pngFile = saveBitmapAsPng(
                bitmap,
                outputDirectory,
                "PdfToImage-$pdfName-page-${pageNum + 1}.png"
            )

            val pdfAnnotations = getAnnotationsFrom(pdfPath, pageNum = pageNum + 1)
            val shapes = getShapesFor(pdfAnnotations, page.height)
            jsonShapes = mapPdfShapesToJsonString(shapes as List<Rectangle>)
            page.close()
        }
        return PdfToImageResultData(File(pdfPath), pngFile, pageHeight, jsonShapes)
    }

    private fun getSeekableFileDescriptor(pdfPath: String): ParcelFileDescriptor? {
        var fd: ParcelFileDescriptor? = null
        try {
            fd = ParcelFileDescriptor.open(File(pdfPath), ParcelFileDescriptor.MODE_READ_ONLY)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }
        return fd
    }

    private fun saveBitmapAsPng(bitmap: Bitmap, directory: String, fileName: String): File {
        val file = File(directory, fileName)
        try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return file
    }

    private fun extractFileNameFromPath(filePath: String): String {
        val file = File(filePath)
        return file.nameWithoutExtension
    }

    @JvmStatic
    private fun convertPngShapesToPdfAnnotations(
        shapes: List<Shape>,
        pageHeight: Int,
    ): List<Annotation> = shapes.map { it.toAnnotation(pageHeight) }

    @JvmStatic
    private fun drawPngShapesToPdf(
        pdfFile: File, pageHeight: Int,
        shapes: List<Shape>,
    ) {
        val annotations = convertPngShapesToPdfAnnotations(shapes, pageHeight)
        annotations.forEach { annotation ->
            when (annotation.type) {
                AnnotationType.SQUARE.name -> AnnotationManager.addRectangleAnnotation(
                    annotation.points,
                    pdfFile,
                    annotation.relations
                )

                else -> logError(TAG, "Annotation $annotation is not recognised")
            }
        }
    }
}