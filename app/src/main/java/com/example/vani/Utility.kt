package com.example.vani

import android.os.Build
import android.util.Rational

object Utility {

    fun supportsPiPMode(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
    }

}