package com.example.k2022_03_09_radio

import VolumeControlHelper
import android.content.ContentValues.TAG
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.nfc.tech.MifareUltralight.PAGE_SIZE
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*
import java.io.IOException

class MainActivity : AppCompatActivity() {
    private lateinit var currentStationLayout: RelativeLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var radioAdapter: RadioAdapter
    private lateinit var stationImageView: ImageView
    private lateinit var stationNameTextView: TextView
    private lateinit var playPauseButton: ImageButton
    private lateinit var volumeSeekBar: SeekBar
    private lateinit var muteButton: ImageButton

    private lateinit var TOTAL_RADIO_STATIONS: List<RadioStation>
    private lateinit var volumeControlHelper: VolumeControlHelper
    private lateinit var audioManager: AudioManager
    private var currentStation: RadioStation? = null
    private var mediaPlayerJob: Job? = null
    private var currentPage = 0
    private var isLoading = false
    private var mediaPlayer: MediaPlayer? = null
    private var radioOn: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        volumeSeekBar = findViewById(R.id.volumeSeekBar)
        muteButton = findViewById(R.id.muteButton)
        currentStationLayout = findViewById(R.id.currentStationLayout)
        stationImageView = findViewById(R.id.stationImageView)
        stationNameTextView = findViewById(R.id.stationNameTextView)
        playPauseButton = findViewById(R.id.playPauseButton)
        recyclerView = findViewById(R.id.recyclerView)

        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        volumeControlHelper = VolumeControlHelper(audioManager)
        recyclerView.layoutManager = LinearLayoutManager(this)


        // Initialize the mute button based on the mute status
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val isMuted = currentVolume == 0
        updateMuteButton(isMuted)

        TOTAL_RADIO_STATIONS = listOf(
            RadioStation("WHUS FM", "http://stream.whus.org:8000/whusfm", R.drawable.radiostation),
            RadioStation(
                "FREEDOM FM",
                "https://edge3.audioxi.com/FREEDOMFM",
                R.drawable.radiostation
            ),
            RadioStation(
                "HIT RADIO",
                "http://hitradio-maroc.ice.infomaniak.ch/hitradio-maroc-128.mp3",
                R.drawable.radiostation
            ),
            RadioStation(
                "WORLDWIDE FM",
                "https://worldwidefm.out.airtime.pro/worldwidefm_a",
                R.drawable.radiostation
            ),
            RadioStation(
                "TIMES RADIO",
                "https://timesradio.wireless.radio/stream",
                R.drawable.radiostation
            ),
            RadioStation(
                "TIMES RADIO",
                "https://timesradio.wireless.radio/stream",
                R.drawable.radiostation
            ),
            RadioStation(
                "TIMES RADIO",
                "https://timesradio.wireless.radio/stream",
                R.drawable.radiostation
            ),
            // Add more radio stations here
        )

        radioAdapter = RadioAdapter(mutableListOf()) { station ->
            if (radioOn) {
                mediaPlayer?.stop()
                mediaPlayer?.reset()
                currentStationLayout.visibility = View.GONE
                radioOn = false
            }
            playRadioStation(station)
            currentStation = station
            updateCurrentStationUI()
            volumeControlHelper.setupVolumeControl(volumeSeekBar)
            volumeControlHelper.setupMuteButton(muteButton)
        }

        recyclerView.adapter = radioAdapter

        playPauseButton.setOnClickListener {
            if (!radioOn) {
                currentStation?.let {
                    playRadioStation(it)
                    updateCurrentStationUI()
                }
            } else {
                if (mediaPlayer != null && mediaPlayer?.isPlaying == true) {
                    mediaPlayer?.pause()
                    playPauseButton.setImageResource(R.drawable.ic_play)
                } else if (mediaPlayer != null) {
                    mediaPlayer?.start()
                    playPauseButton.setImageResource(R.drawable.ic_pause)
                }
                radioOn = !radioOn
            }

        }

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                if (!isLoading && firstVisibleItemPosition + visibleItemCount >= totalItemCount) {
                    loadMoreItems()
                }
            }
        })

        loadMoreItems()
    }

    private fun playRadioStation(station: RadioStation) {
        mediaPlayerJob?.cancel()
        mediaPlayerJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                // Release the previous MediaPlayer instance if it exists
                mediaPlayer?.release()

                // Create a new MediaPlayer instance
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(station.url)
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                    prepareAsync()
                }

                // Set up listeners for prepared and completion events
                mediaPlayer?.setOnPreparedListener { player ->
                    player.start()
                    radioOn = true
                }

                mediaPlayer?.setOnCompletionListener {
                    it.release()
                    radioOn = false
                }
            } catch (e: IOException) {
                // Handle IO errors
                Log.e(TAG, "Error playing radio station: ${e.message}")
            } catch (e: IllegalStateException) {
                // Handle illegal state errors
                Log.e(TAG, "Illegal state error during media playback: ${e.message}")
            } catch (e: Exception) {
                // Handle other exceptions
                Log.e(TAG, "Error during media playback: ${e.message}")
            }
        }
    }

    private fun loadMoreItems() {
        // Simulated API call or data fetch
        val newItems = fetchDataFromApi(currentPage)
        currentPage++

        radioAdapter.addItems(newItems)
        radioAdapter.setLoading(false)
    }

    private fun fetchDataFromApi(page: Int): List<RadioStation> {
        val items = mutableListOf<RadioStation>()
        val startIndex = page * PAGE_SIZE
        val endIndex = minOf(startIndex + PAGE_SIZE, TOTAL_RADIO_STATIONS.size)

        for (i in startIndex until endIndex) {
            items.add(TOTAL_RADIO_STATIONS[i])
        }

        return items
    }


    private fun updateCurrentStationUI() {
        currentStation?.let {
            stationNameTextView.text = it.name
            stationImageView.setImageResource(it.imageResource)
            currentStationLayout.visibility = View.VISIBLE
            playPauseButton.setImageResource(R.drawable.ic_pause)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()// Release the MediaPlayer instance
        mediaPlayerJob?.cancel() // Cancel the coroutine in onDestroy
    }

    fun toggleLayoutVisibility(view: View) {
        val layout = findViewById<RelativeLayout>(R.id.currentStationLayout)
        if (layout.visibility == View.VISIBLE) {
            // Hide the layout
            layout.visibility = View.GONE
        } else {
            // Show the layout
            layout.visibility = View.VISIBLE
        }
    }

    // Function to update the mute button icon based on mute status
    private fun updateMuteButton(isMuted: Boolean) {
        if (isMuted) {
            muteButton.setImageResource(R.drawable.ic_mute)
        } else {
            muteButton.setImageResource(R.drawable.ic_unmute)
        }
    }

}
