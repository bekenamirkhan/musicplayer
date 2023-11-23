package com.example.music

import android.annotation.SuppressLint
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var btnPlayPause: ImageButton
    private lateinit var btnPrevious: ImageButton
    private lateinit var btnNext: ImageButton
    private lateinit var btnChooseSong: Button
    private lateinit var btnShare: ImageButton
    private lateinit var btnShuffle: ImageButton
    private lateinit var seekBar: SeekBar
    private lateinit var volumeBar: SeekBar
    private lateinit var handler: Handler
    private lateinit var coverImage: ImageView
    private lateinit var startTimeTextView: TextView
    private lateinit var currentSongTextView: TextView
    private lateinit var currentSongNameTextView: TextView
    private lateinit var albumImage1: ImageView
    private lateinit var albumImage2: ImageView
    private lateinit var albumImage3: ImageView
    private lateinit var noteImage: ImageView

    private val songs = intArrayOf(R.raw.liltecca_ransom, R.raw.modjo_lady, R.raw.theweeknd_inyoureyes)
    private val songNames = arrayOf("Lil Tecca - Ransom", "Modjo - Lady", "The Weeknd - In Your Eyes")
    private val albumImages = intArrayOf(R.drawable.cover1, R.drawable.cover2, R.drawable.cover3)
    private var currentSongIndex = 0

    private val PICK_AUDIO_REQUEST = 1
    private var currentSongUri: Uri? = null
    private var currentSongTitle: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mediaPlayer = MediaPlayer.create(this, songs[currentSongIndex])
        btnPlayPause = findViewById(R.id.btnPlayPause)
        btnPrevious = findViewById(R.id.btnPrevious)
        btnNext = findViewById(R.id.btnNext)
        btnChooseSong = findViewById(R.id.btnChooseSong)
        btnShare = findViewById(R.id.btnShare)
        btnShuffle = findViewById(R.id.btnShuffle)
        seekBar = findViewById(R.id.seekBar)
        volumeBar = findViewById(R.id.volumeBar)
        coverImage = findViewById(R.id.coverImage)
        startTimeTextView = findViewById(R.id.startTimeTextView)
        currentSongTextView = findViewById(R.id.currentSongTextView)
        currentSongNameTextView = findViewById(R.id.currentSongNameTextView)
        albumImage1 = findViewById(R.id.albumImage1)
        albumImage2 = findViewById(R.id.albumImage2)
        albumImage3 = findViewById(R.id.albumImage3)
        noteImage = findViewById(R.id.coverImage)
        handler = Handler(Looper.getMainLooper())

        volumeBar.max = 100
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        volumeBar.progress = currentVolume

        volumeBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                    val adjustedVolume =
                        (progress.toFloat() / volumeBar.max * maxVolume).toInt()
                    audioManager.setStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        adjustedVolume,
                        AudioManager.FLAG_SHOW_UI
                    )
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        btnPlayPause.setOnClickListener {
            if (mediaPlayer.isPlaying) {
                mediaPlayer.pause()
                btnPlayPause.setImageResource(android.R.drawable.ic_media_play)
            } else {
                mediaPlayer.start()
                btnPlayPause.setImageResource(android.R.drawable.ic_media_pause)
                updateSeekBar()
            }
        }

        btnPrevious.setOnClickListener {
            currentSongIndex = (currentSongIndex - 1 + songs.size) % songs.size
            changeSong()
        }

        btnNext.setOnClickListener {
            currentSongIndex = (currentSongIndex + 1) % songs.size
            changeSong()
        }

        btnChooseSong.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "audio/*"
            startActivityForResult(intent, PICK_AUDIO_REQUEST)
        }

        btnShare.setOnClickListener { shareMusic() }

        btnShuffle.setOnClickListener {
            shuffleSongs()
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mediaPlayer.seekTo(progress * 1000)
                }
                startTimeTextView.text = formatTime(progress * 1000)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        currentSongNameTextView.text = songNames[currentSongIndex]
        albumImage1.setImageResource(albumImages[0])
        albumImage2.setImageResource(albumImages[1])
        albumImage3.setImageResource(albumImages[2])

        albumImage1.setOnClickListener {
            currentSongIndex = 0
            changeSong()
        }

        albumImage2.setOnClickListener {
            currentSongIndex = 1
            changeSong()
        }

        albumImage3.setOnClickListener {
            currentSongIndex = 2
            changeSong()
        }
    }

    private fun changeSong() {
        if (mediaPlayer.isPlaying) {
            mediaPlayer.stop()
            mediaPlayer.release()
        }
        mediaPlayer = if (currentSongIndex < songs.size) {
            MediaPlayer.create(this, songs[currentSongIndex])
        } else {
            MediaPlayer()
        }
        btnPlayPause.setImageResource(android.R.drawable.ic_media_play)
        seekBar.progress = 0

        currentSongNameTextView.text = if (currentSongIndex < songNames.size) {
            songNames[currentSongIndex]
        } else {
            currentSongTitle ?: "Unknown Song"
        }
        if (currentSongIndex < albumImages.size) {
            coverImage.setImageResource(albumImages[currentSongIndex])
        } else {
            coverImage.setImageResource(R.drawable.note_cover)
        }
    }

    private fun updateSeekBar() {
        seekBar.max = mediaPlayer.duration / 1000
        seekBar.progress = mediaPlayer.currentPosition / 1000
        startTimeTextView.text = formatTime(mediaPlayer.currentPosition)
        handler.postDelayed({ updateSeekBar() }, 1000)
    }

    private fun formatTime(milliseconds: Int): String {
        val seconds = milliseconds / 1000
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format("%d:%02d", minutes, remainingSeconds)
    }

    private fun shareMusic() {
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "text/plain"
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Sharing Music")

        val currentSongDescription =
            currentSongUri?.let { getFileName(it) } ?: currentSongTitle
            ?: songNames[currentSongIndex]

        shareIntent.putExtra(Intent.EXTRA_TEXT, "Сейчас я слушаю эту замечательную песню: $currentSongDescription")
        startActivity(Intent.createChooser(shareIntent, "Share via"))
    }

    private fun shuffleSongs() {
        currentSongIndex = (0 until songs.size).random()
        changeSong()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.release()
        handler.removeCallbacksAndMessages(null)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_AUDIO_REQUEST && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                currentSongIndex = songs.size
                currentSongUri = uri
                currentSongTitle = getFileName(uri)
                changeSong()

                mediaPlayer.setDataSource(this, uri)
                mediaPlayer.prepare()
                btnPlayPause.setImageResource(android.R.drawable.ic_media_play)
                seekBar.progress = 0
            }
        }
    }

    @SuppressLint("Range")
    private fun getFileName(uri: Uri): String {
        val contentResolver = contentResolver
        val cursor = contentResolver.query(uri, null, null, null, null)
        return if (cursor != null && cursor.moveToFirst()) {
            val displayName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
            cursor.close()
            displayName
        } else {
            "Unknown Song"
        }
    }
}
