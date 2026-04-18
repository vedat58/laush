package com.example.laush.data

import android.net.Uri
import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

object FirebaseUpload {
    
    private const val TAG = "FirebaseUpload"
    private val storage = FirebaseStorage.getInstance()
    
    suspend fun upload(data: ByteArray, type: String = "image", userId: String): String? = withContext(Dispatchers.IO) {
        try {
            val extension = if (type == "video") "mp4" else "jpg"
            val path = if (type == "video") "videos" else "photos"
            val ref = storage.reference.child("$path/${userId}_${System.currentTimeMillis()}.$extension")
            
            val uploadTask = ref.putBytes(data)
            
            suspendCancellableCoroutine { continuation ->
                uploadTask.addOnSuccessListener { taskSnapshot ->
                    taskSnapshot.storage.downloadUrl.addOnSuccessListener { uri ->
                        Log.d(TAG, "Upload success: ${uri.toString()}")
                        continuation.resume(uri.toString())
                    }
                }.addOnFailureListener { e ->
                    Log.e(TAG, "Upload failed: ${e.message}")
                    continuation.resume(null)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception: ${e.message}")
            null
        }
    }
}