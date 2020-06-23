package com.example.vani

import android.content.Context
import android.os.Build
import android.util.Rational
import android.view.Gravity
import android.widget.Toast

object Utility {

    fun supportsPiPMode(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
    }

    fun toast(context: Context, text:String){
        val toast= Toast.makeText(context, text, Toast.LENGTH_SHORT)
        toast.setGravity(Gravity.TOP or Gravity.CENTER, 0, 0)
        toast.show()
    }


}