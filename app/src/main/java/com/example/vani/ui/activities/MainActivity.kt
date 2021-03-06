package com.example.vani.ui.activities

import android.Manifest
import android.content.res.Configuration
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.util.Rational
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.example.vani.firebase.FirebaseAnalytics
import com.example.vani.firebase.FirebaseRemoteConfiguration
import com.example.vani.R
import com.example.vani.databinding.ActivityMainBinding



class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding


    private lateinit var navigationController: NavController
    private var navHostFragment: Fragment? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        navigationController = Navigation.findNavController(this,
            R.id.nav_host_fragment_container
        )

        FirebaseAnalytics(this)
            .logEvent("MainActivityScreenOnCreate","actionDEfined")
        FirebaseRemoteConfiguration().fetch(this)

        navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment_container)

    }





    private fun getPipRatio(): Rational? {
        val width: Int? = window?.decorView?.width
        val height: Int? = window?.decorView?.height
        return width?.let { height?.let { it1 -> Rational(it, it1) } }
    }

    override fun onUserLeaveHint() {

        /*val orientation:Int = resources.configuration.orientation
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            val navHostFragment = supportFragmentManager.primaryNavigationFragment
            val fragment = navHostFragment?.childFragmentManager?.primaryNavigationFragment
            if (fragment is PlayerFragment) {
                val params: PictureInPictureParams = PictureInPictureParams.Builder()
                    .setAspectRatio(getPipRatio())
                    //.setActions(getPIPActions(getCurrentVideo()))
                    .build()
                enterPictureInPictureMode(params)
            }
        }*/
        super.onUserLeaveHint()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        Log.d("TAG","onConfigurationChanged")
        super.onConfigurationChanged(newConfig)
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        Log.d("TAG","onSaveInstanceState1")

        super.onSaveInstanceState(outState, outPersistentState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        Log.d("TAG","onSaveInstanceState2")

        super.onSaveInstanceState(outState)
    }


}



