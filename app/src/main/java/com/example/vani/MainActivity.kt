package com.example.vani

import android.app.PictureInPictureParams
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.util.Rational
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.example.vani.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding

    private lateinit var navigationController: NavController
    private var navHostFragment: Fragment? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        navigationController = Navigation.findNavController(this, R.id.nav_host_fragment_container)


        navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment_container)


    }

    private fun getPipRatio(): Rational? {
        val width: Int? = window?.decorView?.width
        val height: Int? = window?.decorView?.height
        return width?.let { height?.let { it1 -> Rational(it, it1) } }
    }

    override fun onUserLeaveHint() {

        val orientation = resources.configuration.orientation
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
        }
        super.onUserLeaveHint()
    }


}



