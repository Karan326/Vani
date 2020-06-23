package com.example.vani.ui.activities

import android.annotation.SuppressLint
import android.app.PictureInPictureParams
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.util.Rational
import android.view.ScaleGestureDetector
import android.view.Surface
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.example.vani.R
import com.example.vani.databinding.PlayerLayoutBinding
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.analytics.AnalyticsListener
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultAllocator
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import kotlinx.android.synthetic.main.custom_player_layout.*

class PlayerActivity : AppCompatActivity() {
    lateinit var binding: PlayerLayoutBinding

    private var fullscreenButton: ImageView? = null
    private var player: SimpleExoPlayer? = null
    private val mScaleGestureDetector: ScaleGestureDetector? = null
    private var playbackStateListener: PlaybackAnalyticsListener? = null
    private val TAG: String = PlayerActivity::class.java.name
    private lateinit var uri: Uri
    private var fullscreen = false

    private var playWhenReady = true
    private var currentWindow = 0
    private var playbackPosition: Long = 0



    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = PlayerLayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        uri = Uri.parse(intent?.getStringExtra("uri"))
        playbackStateListener = PlaybackAnalyticsListener()
        fullscreenButton = binding.playerView.findViewById(R.id.exo_fullscreen_icon);


        (fullscreenButton as ImageView).setOnClickListener {
            if (fullscreen) {

                this.window.decorView.systemUiVisibility =
                    View.SYSTEM_UI_FLAG_VISIBLE

                this.requestedOrientation =
                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                fullscreen = false
            } else {
                hideSystemUi()
                this.requestedOrientation =
                    ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                fullscreen = true
            }
        }

    }

    private val MIN_BUFFER_DURATION = 8000
    //Max Video you want to buffer during PlayBack
//Max Video you want to buffer during PlayBack
    val MAX_BUFFER_DURATION = 16000
    //Min Video you want to buffer before start Playing it
//Min Video you want to buffer before start Playing it
    val MIN_PLAYBACK_START_BUFFER = 5000
    //Min video You want to buffer when user resumes video
//Min video You want to buffer when user resumes video
    val MIN_PLAYBACK_RESUME_BUFFER = 8000

    private fun initializePlayer() {
            releasePlayer()
            val trackSelector = DefaultTrackSelector(this)
            val loadControl: LoadControl = DefaultLoadControl.Builder()
            .setAllocator(DefaultAllocator(true, 16))
            .setBufferDurationsMs(
                MIN_BUFFER_DURATION,
                MAX_BUFFER_DURATION,
                MIN_PLAYBACK_START_BUFFER,
                MIN_PLAYBACK_RESUME_BUFFER
            )
            .setTargetBufferBytes(-1)
            .setPrioritizeTimeOverSizeThresholds(true).createDefaultLoadControl()
            /*trackSelector.setParameters(
                trackSelector.buildUponParameters().setMaxVideoSizeSd()
            )*/

            player= this.let {
                trackSelector.let { it1 ->
                    SimpleExoPlayer.Builder(it).setTrackSelector(
                        it1
                    ).setLoadControl(loadControl).build()
                }
            }
         playbackStateListener?.let { (player as SimpleExoPlayer).addAnalyticsListener(it) }
            binding.playerView.player = player

            (binding.playerView.player as SimpleExoPlayer).videoScalingMode =
                C.VIDEO_SCALING_MODE_SCALE_TO_FIT;
         val mediaSource = buildMediaSource(uri)
            (player as SimpleExoPlayer).playWhenReady = playWhenReady
            (player as SimpleExoPlayer).seekTo(currentWindow, playbackPosition)
            mediaSource?.let { (player as SimpleExoPlayer).prepare(it, false, false) }

    }
    private fun buildMediaSource(uri: Uri): MediaSource? {
        // These factories are used to construct two media sources below
        val dataSourceFactory: DataSource.Factory =
            DefaultDataSourceFactory(this, this.let { Util.getUserAgent(it, "vani_player") })
        val mediaSourceFactory = DashMediaSource.Factory(dataSourceFactory)

        val mediaSource: MediaSource?
        when (Util.inferContentType(uri)) {
            C.TYPE_HLS -> {
                mediaSource =
                    ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(uri)
            }
            C.TYPE_OTHER -> {
                mediaSource =
                    ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(uri)
            }
            else -> {
                mediaSource =
                    ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(uri)
            }
        }
        return mediaSource
    }
    private fun releasePlayer() {
        if (player != null) {
            playWhenReady = (player as SimpleExoPlayer).playWhenReady
            playbackPosition = (player as SimpleExoPlayer).currentPosition
            currentWindow = (player as SimpleExoPlayer).currentWindowIndex
            playbackStateListener?.let { (player as SimpleExoPlayer).removeListener(it) }
            (player as SimpleExoPlayer).release()
            player = null
        }
    }

    override fun onStart() {
        super.onStart()
        binding.playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
        //startService()
        initializePlayer()
    }

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onStop() {
        super.onStop()
        super.onStop()
        this.requestedOrientation =
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        releasePlayer()
    }

    override fun onResume() {
        super.onResume()
        hideSystemUi()
    }

    private fun getPipRatio(): Rational? {
        val width: Int? = window?.decorView?.width
        val height: Int? = window?.decorView?.height
        return width?.let { height?.let { it1 -> Rational(it, it1) } }
    }

    override fun onUserLeaveHint() {

        val orientation:Int = resources.configuration.orientation
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
          /*  val navHostFragment = supportFragmentManager.primaryNavigationFragment
            val fragment = navHostFragment?.childFragmentManager?.primaryNavigationFragment
            if (fragment is PlayerFragment) {*/
                val params: PictureInPictureParams = PictureInPictureParams.Builder()
                    .setAspectRatio(getPipRatio())
                    //.setActions(getPIPActions(getCurrentVideo()))
                    .build()
                enterPictureInPictureMode(params)
            //}
        }
        super.onUserLeaveHint()
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode)

        if (isInPictureInPictureMode) {

            //this.supportActionBar?.hide()
            seekbar.visibility = View.GONE
            Log.d("testing", "entered pip mode")

        } else {
            //this.supportActionBar?.show()
            seekbar.visibility = View.VISIBLE

            Log.d("testing", "exit pip mode")
        }
    }

    private fun hideSystemUi() {
        binding.playerView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LOW_PROFILE
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
    }

    private inner class PlaybackAnalyticsListener : AnalyticsListener,
        Player.EventListener {
        override fun onRenderedFirstFrame(
            eventTime: AnalyticsListener.EventTime,
            surface: Surface?
        ) {
        }

        override fun onPlayerStateChanged(
            eventTime: AnalyticsListener.EventTime,
            playWhenReady: Boolean,
            playbackState: Int
        ) {
            val stateString: String
            stateString = when (playbackState) {
                ExoPlayer.STATE_IDLE -> "ExoPlayer.STATE_IDLE      -"
                ExoPlayer.STATE_BUFFERING -> "ExoPlayer.STATE_BUFFERING -"
                ExoPlayer.STATE_READY -> "ExoPlayer.STATE_READY     -"
                ExoPlayer.STATE_ENDED -> "ExoPlayer.STATE_ENDED     -"
                else -> "UNKNOWN_STATE             -"
            }
            Log.d(
                TAG, "changed state to " + stateString
                        + " playWhenReady: " + playWhenReady
            )
        }

        override fun onPlayerStateChanged(
            playWhenReady: Boolean,
            playbackState: Int
        ) {
            val stateString: String
            stateString = when (playbackState) {
                ExoPlayer.STATE_IDLE -> "ExoPlayer.STATE_IDLE      -"
                ExoPlayer.STATE_BUFFERING -> "ExoPlayer.STATE_BUFFERING -"
                ExoPlayer.STATE_READY -> "ExoPlayer.STATE_READY     -"
                ExoPlayer.STATE_ENDED -> "ExoPlayer.STATE_ENDED     -"
                else -> "UNKNOWN_STATE             -"
            }
            Log.d(
                TAG, "changed state to " + stateString
                        + " playWhenReady: " + playWhenReady
            )
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        Log.d(TAG,"onConfigurationChanged")
        super.onConfigurationChanged(newConfig)
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        Log.d(TAG,"onSaveInstanceState1")

        super.onSaveInstanceState(outState, outPersistentState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        Log.d(TAG,"onSaveInstanceState2")

        super.onSaveInstanceState(outState)
    }



}