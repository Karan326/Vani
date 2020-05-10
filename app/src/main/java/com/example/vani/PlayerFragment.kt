package com.example.vani

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.example.vani.databinding.PlayerLayoutBinding
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.analytics.AnalyticsListener
import com.google.android.exoplayer2.analytics.AnalyticsListener.EventTime
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory

class PlayerFragment : Fragment() {

    private var fullscreenButton: ImageView? = null
    private lateinit var binding: PlayerLayoutBinding
    private var player: SimpleExoPlayer? = null

    private var playbackStateListener: PlaybackAnalyticsListener? = null
    private val TAG: String = PlayerFragment::class.java.name

    private lateinit var uri: Uri
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = PlayerLayoutBinding.inflate(inflater)
        return binding.root

    }

    var fullscreen = false

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        uri = Uri.parse(arguments?.getString("uri"))
        playbackStateListener = PlaybackAnalyticsListener()
        fullscreenButton = binding.playerView.findViewById(R.id.exo_fullscreen_icon);


        (fullscreenButton as ImageView).setOnClickListener {
            if (fullscreen) {

                (activity as MainActivity).window.decorView.systemUiVisibility =
                    View.SYSTEM_UI_FLAG_VISIBLE

                (activity as MainActivity).requestedOrientation =
                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                fullscreen = false
            } else {
                hideSystemUi()
                (activity as MainActivity).requestedOrientation =
                    ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                fullscreen = true
            }
        }
    }


    private fun buildMediaSource(uri: Uri): MediaSource {
        // These factories are used to construct two media sources below
        val dataSourceFactory: DataSource.Factory = DefaultDataSourceFactory(context, "vani_player")
        val mediaSourceFactory = DashMediaSource.Factory(dataSourceFactory)
        val mediaSourceFactory1 =
            ProgressiveMediaSource.Factory(dataSourceFactory)

        // Create a media source using the supplied URI
        val mediaSource1: MediaSource = mediaSourceFactory1.createMediaSource(uri)

        // Additionally create a media source using an MP3
        //  val audioUri = Uri.parse(getString(R.string.media_url_mp3))
        // val mediaSource2: MediaSource = mediaSourceFactory1.createMediaSource(audioUri)
        return mediaSource1
    }

    private fun initializePlayer() {
        if (player == null) {
            val trackSelector = DefaultTrackSelector()
            trackSelector.setParameters(
                trackSelector.buildUponParameters().setMaxVideoSizeSd()
            )
            player = ExoPlayerFactory.newSimpleInstance(context, trackSelector)
            (player as SimpleExoPlayer).addAnalyticsListener(playbackStateListener)
            binding.playerView.player = player
            //val uri = Uri.parse(getString(R.string.media_url_dash))
            val mediaSource = buildMediaSource(uri)
            (player as SimpleExoPlayer).playWhenReady = playWhenReady
            (player as SimpleExoPlayer).seekTo(currentWindow, playbackPosition)
            (player as SimpleExoPlayer).prepare(mediaSource, false, false)
        }
    }


    override fun onResume() {
        super.onResume()
        hideSystemUi()
    }

    override fun onStart() {
        super.onStart()
        initializePlayer()
    }

    override fun onStop() {
        super.onStop()
        releasePlayer()
    }

    @SuppressLint("InlinedApi")
    private fun hideSystemUi() {
        binding.playerView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LOW_PROFILE
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
    }


    private var playWhenReady = true
    private var currentWindow = 0
    private var playbackPosition: Long = 0

    private fun releasePlayer() {
        if (player != null) {
            playWhenReady = (player as SimpleExoPlayer).playWhenReady
            playbackPosition = (player as SimpleExoPlayer).currentPosition
            currentWindow = (player as SimpleExoPlayer).currentWindowIndex
            (player as SimpleExoPlayer).removeListener(playbackStateListener)
            (player as SimpleExoPlayer).release()
            player = null
        }
    }

    private inner class PlaybackAnalyticsListener : AnalyticsListener,
        Player.EventListener {
        override fun onRenderedFirstFrame(
            eventTime: EventTime,
            surface: Surface?
        ) {
        }

        override fun onPlayerStateChanged(
            eventTime: EventTime,
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
}