package com.github.barteksc.sample

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.net.toFile
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

@Throws(IOException::class)
fun Context.copyUriToFile(uri: Uri, destinationFile: File) {
    contentResolver.openInputStream(uri)?.use { inputStream ->
        FileOutputStream(destinationFile).use { outputStream ->
            inputStream.copyTo(outputStream)
        }
    }
}

fun Uri.toFileOrNull(context: Context, fileName: String?): File? {
    if (fileName == null) return null

    val destinationDir = context.filesDir
    val destinationFile = File(destinationDir, fileName)

    return try {
        context.copyUriToFile(this, destinationFile)
        destinationFile
    } catch (e: IOException) {
        e.printStackTrace()
        null
    }
}

fun Uri.getFileName(context: Context): String? = try {
    if (scheme == "content") {
        context.contentResolver.query(
            this,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex == -1) return@use null
            cursor.moveToFirst()
            cursor.getString(nameIndex) ?: lastPathSegment
        }
    } else toFile().name
} catch (e: Exception) {
    null
}