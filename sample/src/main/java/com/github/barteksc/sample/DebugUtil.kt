package com.github.barteksc.sample

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.webkit.URLUtil
import android.widget.Toast
import androidx.core.net.toFile
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


internal fun Context.toast(text: String) {
    Toast.makeText(this, text, Toast.LENGTH_LONG).show()
}

internal fun logDebug(tag: String = "", text: String) {
    Log.d(tag, text)
}

internal fun logInfo(tag: String = "", text: String) {
    Log.i(tag, text)
}

internal fun logError(tag: String = "", text: String) {
    Log.e(tag, text)
}

@Throws(IOException::class)
fun Context.copyUriToFile(uri: Uri, destinationFile: File) {
    contentResolver.openInputStream(uri)?.use { inputStream ->
        FileOutputStream(destinationFile).use { outputStream ->
            val buffer = ByteArray(4 * 1024)
            var read: Int
            while (inputStream.read(buffer).also { read = it } != -1) {
                outputStream.write(buffer, 0, read)
            }
            outputStream.flush()
        }
    }
}

fun Uri.toFileOrNull(context: Context, fileName: String?): File? {
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
    when (scheme) {
        "content" ->
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

        "https" -> {
            val contentDisposition = null
            val mimeType = null
            URLUtil.guessFileName(path, contentDisposition, mimeType)
        }

        else -> toFile().name
    }
} catch (e: Exception) {
    null
}