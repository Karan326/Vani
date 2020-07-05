package com.example.vani.ui.activities

import android.annotation.SuppressLint
import android.app.PictureInPictureParams
import android.content.ContentResolver
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Point
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.os.PersistableBundle
import android.provider.Settings
import android.provider.Settings.SettingNotFoundException
import android.util.DisplayMetrics
import android.util.Log
import android.util.Rational
import android.view.*
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.BindingAdapter
import androidx.databinding.BindingMethods
import com.example.vani.R
import com.example.vani.Utility
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
import kotlinx.android.synthetic.main.custom_player_layout.view.*
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.ceil

class PlayerActivity : AppCompatActivity(), View.OnTouchListener {
    private var mediavolume: Int=0
    private var cResolver: ContentResolver? = null
    private var windoW:Window?= null

    private var screen_swipe_move: Boolean=false
    private var intLeft: Boolean = false
    private var intRight: Boolean = false

    private var sWidth: Int? = null
    private var sHeight: Int? = null
    private var device_width: Int? = null
    private var device_height: Int? = null

    private var seekSpeed = 0.0

    private var baseX = 0f
    private var baseY = 0f
    private var diffX: Long = 0
    private var diffY: Long = 0
    private var calculatedTime = 0
    private var seekDur: String? = null
    private var intTop: Boolean = false
    private var intBottom: Boolean = false
    private var size: Point? = null
    private var display: Display? = null
    lateinit var binding: PlayerLayoutBinding

    private var player: SimpleExoPlayer? = null
    private val mScaleGestureDetector: ScaleGestureDetector? = null
    private var playbackStateListener: PlaybackAnalyticsListener? = null
    private val TAG: String = PlayerActivity::class.java.name
    private lateinit var uri: Uri
    private var name: String? = null
    private var fullscreen = false
    private var playWhenReady = true
    private var currentWindow = 0
    private var playbackPosition: Long = 0


    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = PlayerLayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        isPipModeOn()
        uri = Uri.parse(intent?.getStringExtra("uri"))
        name = intent?.getStringExtra("name")
        playbackStateListener = PlaybackAnalyticsListener()
        setListeners()
        setFullScreen()
    }

    @BindingAdapter("android:paddingLeft")
    internal fun View.bindPadding(paddingLeft:Int){
        setPadding(
            paddingLeft,
            paddingTop,
            paddingRight,
            paddingBottom
        )
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setListeners() {
        setOnFullScreenButtonListener()
        setOnBackButtonListener()
        binding.playerView.setOnTouchListener(this)
        //binding.playerView.bindPadding(2)
        setOnCropListener()
    }

    private fun setOnCropListener() {

        binding.playerView.resizeScreen.setOnClickListener {

            if (binding.playerView.resizeMode == AspectRatioFrameLayout.RESIZE_MODE_ZOOM) {
                binding.playerView.resizeMode =
                    AspectRatioFrameLayout.RESIZE_MODE_FILL
                Utility.toast(this, "Scaled to fill ")
            } else if (binding.playerView.resizeMode == AspectRatioFrameLayout.RESIZE_MODE_FILL) {
                binding.playerView.resizeMode =
                    AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                Utility.toast(this, "Scaled to zoom ")
            }
            // binding.playerView.invalidate()
        }
    }

    private fun setFullScreen() {
        hideSystemUi()
        this.requestedOrientation =
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        fullscreen = true
    }

    private fun displaySizeAndMetrics() {
        display = windowManager.defaultDisplay
        size = Point()
        display?.getSize(size)
        sWidth = size?.x
        sHeight = size?.y

        val displaymetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displaymetrics)
        device_height = displaymetrics.heightPixels
        device_width = displaymetrics.widthPixels
    }


    private fun setOnBackButtonListener() {
        binding.playerView.bpVideo.setOnClickListener {
            onBackPressed()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        releasePlayer()
    }

    private fun setOnFullScreenButtonListener() {
        binding.playerView.exo_fullscreen_icon.setOnClickListener {
            if (fullscreen) {

                this.window?.decorView?.systemUiVisibility =
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

    private fun isPipModeOn() {
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
        displaySizeAndMetrics()
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

        player = this.let {
            trackSelector.let { it1 ->
                SimpleExoPlayer.Builder(it).setTrackSelector(
                    it1
                ).setLoadControl(loadControl).build()
            }
        }
        playbackStateListener?.let { (player as SimpleExoPlayer).addAnalyticsListener(it) }
        binding.playerView.player = player



        binding.playerView.resizeMode =
            AspectRatioFrameLayout.RESIZE_MODE_FILL
        /* (binding.playerView.player as SimpleExoPlayer).videoScalingMode =
             C.VIDEO_SCALING_MODE_SCALE_TO_FIT;*/

        binding.playerView.nameVideo.text = name
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

        val orientation: Int = resources.configuration.orientation
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
        Log.d(TAG, "onConfigurationChanged")
        super.onConfigurationChanged(newConfig)
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        Log.d(TAG, "onSaveInstanceState1")

        super.onSaveInstanceState(outState, outPersistentState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        Log.d(TAG, "onSaveInstanceState2")

        super.onSaveInstanceState(outState)
    }
    private val MIN_DISTANCE = 150
    private var brightness = 0
    private val audioManager: AudioManager? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        when (event?.action) {
            //TOUCH STARTED
            MotionEvent.ACTION_DOWN -> {
                if (event.x < (sWidth?.div(2))!!) {
                    intLeft = true
                    intRight = false
                } else if (event.x > (sWidth!! / 2)) {
                    intLeft = false
                    intRight = true
                }
                val upperLimit = (sHeight?.div(4))?.plus(100)
                val lowerLimit = ((sHeight?.div(4))?.times(3))?.minus(150)
                when {
                    event.y < upperLimit!! -> {
                        intBottom = false
                        intTop = true
                    }
                    event.y > lowerLimit!! -> {
                        intBottom = true
                        intTop = false
                    }
                    else -> {
                        intBottom = false
                        intTop = false
                    }
                }
                seekSpeed = (player?.duration?.let { TimeUnit.MILLISECONDS.toSeconds(it) }
                    ?.times(0.1)!!)
                diffX = 0
                calculatedTime = 0;
                seekDur = String.format(
                    "%02d:%02d",
                    TimeUnit.MILLISECONDS.toMinutes(diffX) -
                            TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(diffX)),
                    TimeUnit.MILLISECONDS.toSeconds(diffX) -
                            TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(diffX))
                )


                baseX = event.x
                baseY = event.y
            }
            //TOUCH IS MOVED IN DIRECTION
            MotionEvent.ACTION_MOVE -> {
                screen_swipe_move = true
                //if (controlsState == FULLCONTORLS) {
                   // root.setVisibility(View.GONE)
                    diffX = Math.ceil(event.x - baseX.toDouble()).toLong()
                    diffY = Math.ceil(event.y - baseY.toDouble()).toLong()
                    val brightnessSpeed = 0.05
                    if (Math.abs(diffY) > MIN_DISTANCE) {
                        //tested_ok = true
                    }
                    if (abs(diffY) > abs(diffX)) {
                        if (intLeft) {
                            cResolver = contentResolver
                            windoW = window
                            try {
                                Settings.System.putInt(
                                    cResolver,
                                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
                                )
                                brightness = Settings.System.getInt(
                                    cResolver,
                                    Settings.System.SCREEN_BRIGHTNESS
                                )
                            } catch (e: SettingNotFoundException) {
                                e.printStackTrace()
                            }
                            var newBrightness =
                                (brightness - diffY * brightnessSpeed)
                            if (newBrightness > 250) {
                                newBrightness = 250.0
                            } else if (newBrightness < 1) {
                                newBrightness = 1.0
                            }
                            val brightPerc = ceil(newBrightness.toDouble() / 250.toDouble() * 100.toDouble())
                            binding.playerView.brightness.visibility = View.VISIBLE
                            binding.playerView.volume_brighness_container.visibility = View.VISIBLE
                            binding.playerView.brightnessSlider.progress = brightPerc.toInt()
                            if (brightPerc < 30) {
                                binding.playerView.brightness_icon.setImageResource(R.drawable.ic_brightness_low)
                                binding.playerView.volume_brighness_icon.setImageResource(R.drawable.ic_brightness_low)

                            } else if (brightPerc > 30 && brightPerc < 80) {
                                binding.playerView.brightness_icon.setImageResource(R.drawable.ic__brightness_medium)
                                binding.playerView.volume_brighness_icon.setImageResource(R.drawable.ic__brightness_medium)

                            } else if (brightPerc > 80) {
                                binding.playerView.brightness_icon.setImageResource(R.drawable.ic_brightness_full)
                                binding.playerView.volume_brighness_icon.setImageResource(R.drawable.ic_brightness_full)

                            }
                            binding.playerView.volume_brightness_percent.text = " " + brightPerc.toInt()
                            Settings.System.putInt(
                                cResolver,
                                Settings.System.SCREEN_BRIGHTNESS,
                                newBrightness.toInt()
                            )
                            val layoutpars = window?.attributes
                            layoutpars?.screenBrightness = brightness / 255.toFloat()
                            window?.attributes = layoutpars
                        } else if (intRight) {
                            if (audioManager != null) {
                                mediavolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                            }
                            val maxVol: Int =
                                audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC)?:100
                            val cal =
                                diffY.toDouble() * (maxVol.toDouble() / (device_height!! * 4).toDouble())
                            var newMediaVolume: Int = mediavolume - cal.toInt()
                            if (newMediaVolume > maxVol) {
                                newMediaVolume = maxVol
                            } else if (newMediaVolume < 1) {
                                newMediaVolume = 0
                            }
                            audioManager?.setStreamVolume(
                                AudioManager.STREAM_MUSIC,
                                newMediaVolume,
                                AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE
                            )
                            val volPerc =
                                ceil(newMediaVolume.toDouble() / maxVol.toDouble() * 100.toDouble())
                            binding.playerView.volume_brightness_percent.text = " " + volPerc.toInt()
                            binding.playerView.volume.visibility = View.VISIBLE
                            binding.playerView.volume_brighness_container.visibility = View.VISIBLE
                            binding.playerView.brightnessSlider.progress = volPerc.toInt()
                            if (volPerc < 1) {
                                binding.playerView.volume_icon.setImageResource(R.drawable.ic_volume_off)
                                binding.playerView.volume_brighness_icon.setImageResource(R.drawable.ic_volume_off)
                                binding.playerView.volume_brighness_container.visibility = View.GONE
                            } else if (volPerc >= 1) {
                                binding.playerView.volume_icon.setImageResource(R.drawable.ic_volume_up_full)
                                binding.playerView.volume_brighness_icon.setImageResource(R.drawable.ic_volume_up_full)
                                binding.playerView.volume_brighness_container.visibility = View.VISIBLE
                            }
                            binding.playerView.volume_brighness_container.visibility = View.VISIBLE
                            binding.playerView.volumeSlider.progress = volPerc.toInt()
                        }
                    } /*else if (Math.abs(diffX) > Math.abs(diffY)) {
                        if (Math.abs(diffX) > com.andromeda.kunalbhatia.demo.hungamaplayer.VideoPlayer.MIN_DISTANCE + 100) {
                            tested_ok = true
                            root.setVisibility(View.VISIBLE)
                            seekBar_center_text.setVisibility(View.VISIBLE)
                            onlySeekbar.setVisibility(View.VISIBLE)
                            top_controls.setVisibility(View.GONE)
                            bottom_controls.setVisibility(View.GONE)
                            var totime = ""
                            calculatedTime = (diffX * seekSpeed).toInt()
                            if (calculatedTime > 0) {
                                seekDur = String.format(
                                    "[ +%02d:%02d ]",
                                    TimeUnit.MILLISECONDS.toMinutes(
                                        calculatedTime.toLong()
                                    ) -
                                            TimeUnit.HOURS.toMinutes(
                                                TimeUnit.MILLISECONDS.toHours(
                                                    calculatedTime.toLong()
                                                )
                                            ),
                                    TimeUnit.MILLISECONDS.toSeconds(
                                        calculatedTime.toLong()
                                    ) -
                                            TimeUnit.MINUTES.toSeconds(
                                                TimeUnit.MILLISECONDS.toMinutes(
                                                    calculatedTime.toLong()
                                                )
                                            )
                                )
                            } else if (calculatedTime < 0) {
                                seekDur = String.format(
                                    "[ -%02d:%02d ]",
                                    Math.abs(
                                        TimeUnit.MILLISECONDS.toMinutes(
                                            calculatedTime.toLong()
                                        ) -
                                                TimeUnit.HOURS.toMinutes(
                                                    TimeUnit.MILLISECONDS.toHours(
                                                        calculatedTime.toLong()
                                                    )
                                                )
                                    ),
                                    Math.abs(
                                        TimeUnit.MILLISECONDS.toSeconds(
                                            calculatedTime.toLong()
                                        ) -
                                                TimeUnit.MINUTES.toSeconds(
                                                    TimeUnit.MILLISECONDS.toMinutes(
                                                        calculatedTime.toLong()
                                                    )
                                                )
                                    )
                                )
                            }
                            totime = String.format(
                                "%02d:%02d",
                                TimeUnit.MILLISECONDS.toMinutes(player!!.currentPosition + calculatedTime) -
                                        TimeUnit.HOURS.toMinutes(
                                            TimeUnit.MILLISECONDS.toHours(
                                                player!!.currentPosition + calculatedTime
                                            )
                                        ),  // The change is in this line
                                TimeUnit.MILLISECONDS.toSeconds(player!!.currentPosition + calculatedTime) -
                                        TimeUnit.MINUTES.toSeconds(
                                            TimeUnit.MILLISECONDS.toMinutes(
                                                player!!.currentPosition + calculatedTime
                                            )
                                        )
                            )
                            txt_seek_secs.setText(seekDur)
                            txt_seek_currTime.setText(totime)
                            seekBar.setProgress((player!!.currentPosition + calculatedTime).toInt())
                        }
                    }*/
             //   }

            }
            //TOUCH IS UP OR OTHER CONTROL TAKEN
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {

                binding.playerView.brightness.visibility = View.GONE
                binding.playerView.volume.visibility = View.GONE
                binding.playerView.volume_brighness_container.visibility = View.GONE

            }

        }
        return super.onTouchEvent(event)
    }

}