package com.r114358.rosette.utils

import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import android.util.Log
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream
import com.r114358.rosette.ModelInfo



const val tag = "rosette-utils"



fun ensureModel(
    cacheDir: File,
    model: ModelInfo,
    force: Boolean = false,
): File {

    val suffix = model.url.substringAfterLast('.', "")
    val fileName = "${model.fileName}.${suffix}"
    val dstFile = File(cacheDir, fileName)
    Log.d(tag, "${dstFile.absolutePath}")

    val needDownload = force || !dstFile.exists()
    if (needDownload) {
        Log.d(tag, "⏬ Downloading ${model.fileName} …")
        downloadFile(model.url, dstFile)
        Log.d(tag, "${model.fileName} saved to ${dstFile.length() / 1_048_576} MB")

        Log.d(tag, dstFile.absolutePath)
        if (dstFile.extension == "zip") {
            Log.d(tag, "📦 Extracting ${dstFile.name} …")
            unzip(dstFile, cacheDir)
//            dstFile.delete()
            return File(cacheDir, dstFile.nameWithoutExtension)
        }
    } else {
        Log.d(tag, "${model.fileName} already present (${dstFile.length() / 1_048_576} MB)")
    }

    return dstFile
}

/** Simple blocking download via OkHttp. Throws if HTTP not 200. */
private fun downloadFile(url: String, dest: File) {
    val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)
        .writeTimeout(0, TimeUnit.SECONDS)
        .callTimeout(0, TimeUnit.SECONDS)
        .build()
    val req = Request.Builder().url(url).build()

    httpClient.newCall(req).execute().use { resp ->
        if (!resp.isSuccessful) error("HTTP ${resp.code}: $url")

        val totalBytes = resp.body!!.contentLength()
        dest.parentFile?.mkdirs()

        var bytesCopied = 0L
        var nextPct = 25

        resp.body!!.byteStream().use { input ->
            FileOutputStream(dest).use { output ->
                val buf = ByteArray(DEFAULT_BUFFER_SIZE)
                var read: Int
                while (input.read(buf).also { read = it } != -1) {
                    output.write(buf, 0, read)
                    bytesCopied += read

                    // only compute % if we know the total length
                    if (totalBytes > 0) {
                        val pct = (bytesCopied * 100 / totalBytes).toInt()
                        if (pct in nextPct..99) {
                            Log.d(tag, "⏬ ${dest.name} $pct% downloaded")
                            nextPct += 25
                        }
                    }
                }
            }
        }
        if (totalBytes > 0) println("✅ ${dest.name} 100% downloaded")
    }
}

private fun unzip(zipFile: File, destDir: File) {
    ZipInputStream(zipFile.inputStream()).use { zis ->
        var entry = zis.nextEntry
        while (entry != null) {
            // Build the path for the current entry
            val outPath = File(destDir, entry.name)

            if (entry.isDirectory) {
                outPath.mkdirs()
            } else {
                // Ensure parent dirs exist
                outPath.parentFile?.mkdirs()
                // Stream copy the entry’s bytes to disk
                Files.copy(zis, outPath.toPath(), REPLACE_EXISTING)
            }
            zis.closeEntry()
            entry = zis.nextEntry
        }
    }
}
