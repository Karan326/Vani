package com.example.vani.ui.fragments

import android.Manifest
import android.app.Activity
import android.content.ContentUris
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.vani.ui.pojos.Video
import com.example.vani.ui.adapters.VideoListAdapter
import com.example.vani.databinding.VideoListLayoutBinding
import com.example.vani.firebase.FirebaseAnalytics
import com.example.vani.ui.activities.PlayerActivity
import kotlinx.coroutines.*

class VideoListFragment : Fragment(),
    VideoListAdapter.CallbackToVideoListFragment {

    private lateinit var videoListAdapter: VideoListAdapter
    private lateinit var binding: VideoListLayoutBinding

    private val videoList = mutableListOf<Video>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = VideoListLayoutBinding.inflate(inflater)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        videoListAdapter = VideoListAdapter()
        val mLayoutManager = GridLayoutManager(context, 2, RecyclerView.VERTICAL, false)
        binding.videoListRV.layoutManager = mLayoutManager
        videoListAdapter.setCallBacktoVideoListFragment(this)
        binding.videoListRV.adapter = videoListAdapter
        setupPermissions()


    }

    private fun setupPermissions() {
        val permissionRead = activity?.let {
            ContextCompat.checkSelfPermission(
                it,
                Manifest.permission.READ_EXTERNAL_STORAGE )
        }

        val permissionWrite= activity?.let {
            ContextCompat.checkSelfPermission(
                it,
                Manifest.permission.WRITE_EXTERNAL_STORAGE )
        }

        if (permissionRead != PackageManager.PERMISSION_GRANTED
            && permissionWrite!= PackageManager.PERMISSION_GRANTED) {
            Log.i("TAG", "Permission to read denied")
            makeRequest(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE))
        }
        else if(permissionRead != PackageManager.PERMISSION_GRANTED){
            makeRequest(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
        }else if(permissionRead != PackageManager.PERMISSION_GRANTED){
            makeRequest(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE))
        }else{
            initLists()
        }
    }

    private fun makeRequest(arrayOf: Array<String>) {
        requestPermissions(arrayOf,
                1)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {

        when (requestCode) {
            1 -> {

                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {

                    Log.i("TAG", "Permission has been denied by user")
                } else {
                    Log.i("TAG", "Permission has been granted by user")
                    initLists()
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun initLists() {
        CoroutineScope(Dispatchers.Main).launch { initialiseList() }
    }

    private suspend fun initialiseList() {

        withContext(Dispatchers.IO) {
            val listJOB = async {

                val projection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    arrayOf(
                        MediaStore.Video.Media._ID,
                        MediaStore.Video.Media.DISPLAY_NAME,
                        MediaStore.Video.Media.SIZE,
                         MediaStore.Video.Media.DURATION
                    )
                } else {
                    arrayOf(
                        MediaStore.Video.Media._ID,
                        MediaStore.Video.Media.DISPLAY_NAME,
                        MediaStore.Video.Media.SIZE
                    )
                }

// Show only videos that are at least 5 minutes in duration.
               // val selection = "${MediaStore.Video.Media.DURATION} >= ?"
               /* val selectionArgs = arrayOf(
                    TimeUnit.MILLISECONDS.convert(5, TimeUnit.MINUTES).toString()
                )*/

// Display videos in alphabetical order based on their display name.
                val sortOrder = "${MediaStore.Video.Media.DISPLAY_NAME} ASC"

                val query = context?.applicationContext?.contentResolver?.query(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    null,
                    null,
                    sortOrder
                )
                query?.use { cursor ->
                    // Cache column indices.
                    val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                    val nameColumn =
                        cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                    val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)

                    val durationColumn= if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                    } else {
                        null
                    }


                    while (cursor.moveToNext()) {
                        // Get values of columns for a given video.
                        var duration:Int?=null
                        val id = cursor.getLong(idColumn)
                        val name = cursor.getString(nameColumn)
                        if(durationColumn!=null) duration = cursor.getInt(durationColumn)
                        val size = cursor.getInt(sizeColumn)

                        val contentUri: Uri = ContentUris.withAppendedId(
                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                            id
                        )

                       /* val thumbnail: Bitmap =
                            requireContext().applicationContext.contentResolver.loadThumbnail(
                                contentUri, Size(640, 480), null
                            )*/

                        // Stores column values and the contentUri in a local object
                        // that represents the media file.

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && duration!=null && duration>3000000){
                        videoList += Video(
                            contentUri,
                            name,
                            size
                        )}
                        else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q){
                            videoList += Video(
                                contentUri,
                                name,
                                size
                            )
                        }
                        Log.d("TAG", name)
                    }
                    videoList
                }
            }
            val list = listJOB.await()
            withContext(Dispatchers.Main) {
                videoListAdapter.updateList(list)
            }

        }


    }

    override fun sendDataAndOpenPlayer(item: Video) {

        val intent= Intent(context, PlayerActivity::class.java)
        intent.putExtra("uri",item.uri.toString())
        intent.putExtra("name",item.name.toString())

        context?.startActivity(intent)
        /*Navigation.findNavController(
            mContext as Activity, R.id.nav_host_fragment_container
        ).navigate(R.id.action_videoListFragment_to_playerFragment, bundle)*/
        FirebaseAnalytics(context as Activity)
            .logEvent("MainActivityScreenOnCreate","actionDEfined")

    }


}