package com.example.vani.firebase

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase

class FirebaseAnalytics(private val mContext: Context) {

    lateinit var firebaseAnalytics: FirebaseAnalytics


    @Synchronized
    fun initializeFA(context: Context?) {
        if (!::firebaseAnalytics.isInitialized)
            firebaseAnalytics = Firebase.analytics
    }

    fun logEvent(event: String, destinationAction: String) {
        initializeFA(mContext)
        val bundle = Bundle()
        bundle.putString(FirebaseAnalytics.Param.DESTINATION, destinationAction)
        firebaseAnalytics.logEvent(event, bundle)

    }

}