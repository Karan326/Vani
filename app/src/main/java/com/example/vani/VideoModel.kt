package com.example.vani

import android.graphics.Bitmap
import android.net.Uri

data class Video(
    val uri: Uri,
    val name: String,
    //val duration: Int,
    val size: Int
    //val thumbnail: Bitmap
)
