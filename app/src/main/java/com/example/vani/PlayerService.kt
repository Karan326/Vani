package com.example.vani

import android.annotation.SuppressLint
import android.app.IntentService
import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.IBinder
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util

class PlayerService : IntentService("PlayerService") {
    private var player: SimpleExoPlayer? = null
    private lateinit var playerNotificationManager: PlayerNotificationManager


    override fun onDestroy() {

        if(::playerNotificationManager.isInitialized){
        playerNotificationManager.setPlayer(null)}
        player?.release()
        player = null

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onHandleIntent(intent: Intent?) {

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val uri = intent?.getParcelableExtra<Uri>("uri")
        if (uri != null) {
            initializePlayer(uri)
        }
        return START_STICKY
    }

    private fun initializePlayer(uri: Uri) {
        if (player == null) {
            val trackSelector = DefaultTrackSelector()
            trackSelector.setParameters(
                trackSelector.buildUponParameters().setMaxVideoSizeSd()
            )
            player = ExoPlayerFactory.newSimpleInstance(this, trackSelector)
            val mediaSource = buildMediaSource(uri)
            (player as SimpleExoPlayer).prepare(mediaSource, false, false)
            (player as SimpleExoPlayer).playWhenReady = true
            // (player as SimpleExoPlayer).seekTo(currentWindow, playbackPosition)


            playerNotificationManager =
                PlayerNotificationManager.createWithNotificationChannel(this,"1", R.string.notification_channel_name,
                    R.string.description_notification, 1,
                    object : PlayerNotificationManager.MediaDescriptionAdapter {
                        override fun createCurrentContentIntent(player: Player): PendingIntent? {
                            val intent = Intent(this@PlayerService, MainActivity::class.java)
                            return PendingIntent.getActivity(
                                this@PlayerService,
                                0,
                                intent,
                                PendingIntent.FLAG_UPDATE_CURRENT
                            )


                        }

                        override fun getCurrentContentText(player: Player): String? {
                            return "hjdhd"
                        }

                        override fun getCurrentContentTitle(player: Player): String {
                            return "jsjsk"
                        }

                        override fun getCurrentLargeIcon(
                            player: Player,
                            callback: PlayerNotificationManager.BitmapCallback
                        ): Bitmap? {

                            return null

                        }
                    })


            playerNotificationManager.setNotificationListener(object :
                PlayerNotificationManager.NotificationListener {


                override fun onNotificationStarted(
                    notificationId: Int,
                    notification: Notification
                ) {
                    startForeground(notificationId, notification)
                }


                override fun onNotificationCancelled(
                    notificationId: Int,
                    dismissedByUser: Boolean
                ) {
                    stopSelf()
                }
            })

            playerNotificationManager.setPlayer(player)
        }


    }

    private fun buildMediaSource(uri: Uri): MediaSource {
        // These factories are used to construct two media sources below
        val dataSourceFactory: DataSource.Factory =
            DefaultDataSourceFactory(this, Util.getUserAgent(this, "vani_player"))
        val mediaSourceFactory1 =
            ProgressiveMediaSource.Factory(dataSourceFactory)
        val mediaSource1: MediaSource = mediaSourceFactory1.createMediaSource(uri)
        return mediaSource1
    }

}