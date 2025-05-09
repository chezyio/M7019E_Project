package com.m7019e.nobi

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ImageCacheWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    companion object {
        const val KEY_IMAGE_URL = "image_url"
        const val KEY_OUTPUT_PATH = "output_path"
    }

    override suspend fun doWork(): Result {
        val imageUrl = inputData.getString(KEY_IMAGE_URL) ?: return Result.failure()
        val outputPath = inputData.getString(KEY_OUTPUT_PATH) ?: return Result.failure()

        return try {
            val file = File(outputPath)
            if (file.exists()) {
                Log.d("ImageCacheWorker", "Image already cached at $outputPath")
                return Result.success(workDataOf(KEY_OUTPUT_PATH to outputPath))
            }

            withContext(Dispatchers.IO) {
                val url = URL(imageUrl)
                val connection = url.openConnection()
                connection.connect()
                val inputStream = connection.getInputStream()
                FileOutputStream(file).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
                inputStream.close()
            }
            Log.d("ImageCacheWorker", "Image cached successfully at $outputPath")
            Result.success(workDataOf(KEY_OUTPUT_PATH to outputPath))
        } catch (e: Exception) {
            Log.e("ImageCacheWorker", "Failed to cache image: ${e.message}", e)
            Result.failure()
        }
    }
}