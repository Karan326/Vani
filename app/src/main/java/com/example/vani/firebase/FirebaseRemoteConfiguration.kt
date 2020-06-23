package com.example.vani.firebase

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.example.vani.ui.activities.MainActivity
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.get
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import java.util.*

class FirebaseRemoteConfiguration {

    lateinit var firebaseRemoteConfig: FirebaseRemoteConfig

    @Synchronized
    fun initializeRC() {
        if (!::firebaseRemoteConfig.isInitialized)
            firebaseRemoteConfig = Firebase.remoteConfig

        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 3600
        }
        firebaseRemoteConfig.setConfigSettingsAsync(configSettings)

        val map: MutableMap<String, Any> = HashMap()

        map["isPipModeOn"] = true
        firebaseRemoteConfig.setDefaultsAsync(map)
    }

    fun setRemoteCOnfif() {
        initializeRC()


    }

    fun fetch(mContext: Context) {
        initializeRC()
        firebaseRemoteConfig.fetchAndActivate().addOnCompleteListener(
            mContext as MainActivity
        ) { task ->
            if (task.isSuccessful) {
                val updated = task.result
                Log.d("TAG", "Config params updated: $updated")

            } else {
                Toast.makeText(
                    mContext, "Fetch failed",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    }

    fun getRemoteConfig(key: String): Any {
        if (!::firebaseRemoteConfig.isInitialized) initializeRC()
        return firebaseRemoteConfig[key]
    }

}







