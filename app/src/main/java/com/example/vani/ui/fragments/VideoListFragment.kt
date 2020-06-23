package com.example.vani.ui.fragments

import android.content.ContentUris
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.vani.ui.pojos.Video
import com.example.vani.ui.adapters.VideoListAdapter
import com.example.vani.databinding.VideoListLayoutBinding
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

        CoroutineScope(Dispatchers.Main).launch { initialiseList() }
    }

    private suspend fun initialiseList() {

        withContext(Dispatchers.IO) {
            val j = async {

                val projection = arrayOf(
                    MediaStore.Video.Media._ID,
                    MediaStore.Video.Media.DISPLAY_NAME,
                    MediaStore.Video.Media.SIZE
                )

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


                    while (cursor.moveToNext()) {
                        // Get values of columns for a given video.
                        val id = cursor.getLong(idColumn)
                        val name = cursor.getString(nameColumn)
                        //val duration = cursor.getInt(durationColumn)
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

                        videoList += Video(
                            contentUri,
                            name,
                            size
                        )
                        Log.d("TAG", name)
                    }
                    videoList
                }
            }
            val list = j.await()
            withContext(Dispatchers.Main) {
                videoListAdapter.updateList(list)
            }

        }


    }

    override fun sendDataAndOpenPlayer(uri: Uri) {




    }


}