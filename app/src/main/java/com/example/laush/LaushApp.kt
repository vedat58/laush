package com.example.laush

import android.app.Application
import android.util.Log
import com.example.laush.data.CloudinaryConfig

class LaushApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.d("LaushApp", "Application started")
        
        CloudinaryConfig.cloudName = "dxwa90qtp"
        CloudinaryConfig.apiKey = ""
        CloudinaryConfig.apiSecret = ""
        CloudinaryConfig.init(this)
    }
}