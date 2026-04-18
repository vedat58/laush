package com.example.laush.data

import com.cloudinary.Cloudinary
import com.cloudinary.android.MediaManager

object CloudinaryConfig {
    var cloudName: String = "YOUR_CLOUD_NAME"
    var apiKey: String = "YOUR_API_KEY"
    var apiSecret: String = "YOUR_API_SECRET"
    
    fun init(context: android.content.Context) {
        val config = mapOf(
            "cloud_name" to cloudName,
            "api_key" to apiKey,
            "api_secret" to apiSecret
        )
        MediaManager.init(context, config)
    }
    
    fun getClient(): Cloudinary {
        return Cloudinary("cloudinary://$apiKey:$apiSecret@$cloudName")
    }
}