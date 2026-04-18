package com.example.laush.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL

object CloudinaryUpload {
    
    private const val TAG = "CloudinaryUpload"
    private const val CLOUD_NAME = "dzwa90qtp"
    
    suspend fun upload(data: ByteArray, type: String = "image", preset: String): String? = withContext(Dispatchers.IO) {
        try {
            val resourceType = if (type == "video") "video" else "image"
            val apiUrl = "https://api.cloudinary.com/v1_1/$CLOUD_NAME/$resourceType/upload"
            
            Log.d(TAG, "Starting upload...")
            Log.d(TAG, "Cloud: $CLOUD_NAME, Preset: $preset, Type: $type, Size: ${data.size}")
            
            val url = URL(apiUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Connection", "Keep-Alive")
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=----CloudinaryBoundary")
            
            val dos = DataOutputStream(conn.outputStream)
            
            // Upload preset
            dos.writeBytes("------CloudinaryBoundary\r\n")
            dos.writeBytes("Content-Disposition: form-data; name=\"upload_preset\"\r\n\r\n")
            dos.writeBytes("$preset\r\n")
            
            // File
            dos.writeBytes("------CloudinaryBoundary\r\n")
            val fileName = if (type == "video") "video.mp4" else "image.jpg"
            val mimeType = if (type == "video") "video/mp4" else "image/jpeg"
            dos.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"$fileName\"\r\n")
            dos.writeBytes("Content-Type: $mimeType\r\n\r\n")
            dos.write(data)
            dos.writeBytes("\r\n")
            
            // End
            dos.writeBytes("------CloudinaryBoundary--\r\n")
            dos.flush()
            dos.close()
            
            val responseCode = conn.responseCode
            Log.d(TAG, "Response: $responseCode")
            
            if (responseCode == 200) {
                val response = conn.inputStream.bufferedReader().readText()
                
                // JSON parsing
                val urlStart = response.indexOf("\"secure_url\":\"")
                if (urlStart != -1) {
                    val urlEnd = response.indexOf("\"", urlStart + 14)
                    val uploadedUrl = response.substring(urlStart + 14, urlEnd).replace("\\/", "/")
                    Log.d(TAG, "SUCCESS! URL: $uploadedUrl")
                    return@withContext uploadedUrl
                }
                Log.e(TAG, "URL not found in response")
                null
            } else {
                val errorMsg = conn.errorStream?.bufferedReader()?.readText() ?: "No error"
                Log.e(TAG, "FAILED: $responseCode - $errorMsg")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "EXCEPTION: ${e.message}")
            e.printStackTrace()
            null
        }
    }
}