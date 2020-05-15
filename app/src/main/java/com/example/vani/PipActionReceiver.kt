package com.example.vani

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class PipActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null){
            Log.d("testing",""+ intent?.action)
        }
    }
}