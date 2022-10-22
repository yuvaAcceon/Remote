package com.example.myapplication

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    lateinit var speechRecognizer: SpeechRecognizer
    lateinit var clMic: ConstraintLayout
    lateinit var clPower: ConstraintLayout
    lateinit var etSpeechText: EditText
    lateinit var tvChannelNumber: TextView
    lateinit var tvVolume: TextView
    lateinit var tvPowerOn: TextView
    lateinit var tvCallCalling: TextView
    lateinit var tvAmbulanceCalling: TextView
    lateinit var clChannelUp: ConstraintLayout
    lateinit var clChannelDown: ConstraintLayout
    lateinit var clVolumeUp: ConstraintLayout
    lateinit var clVolumeDown: ConstraintLayout
    lateinit var clCall: ConstraintLayout
    lateinit var clAmbulance: ConstraintLayout
    private var channelNumber: Int = 0
    var volume: Int = 0
    private val upKeywords = arrayOf("up", "next", "plus", "add", "increase")
    private val downKeywords = arrayOf("down", "previous", "minus", "subtract", "decrease")
    var isPowerOn = true
    var isCalling = false
    var isCallingAmbulance = false

    @SuppressLint("UseCompatLoadingForDrawables", "ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        clMic = findViewById(R.id.cl_mic)
        clPower = findViewById(R.id.cl_power)
        etSpeechText = findViewById(R.id.et_speech_text)
        tvChannelNumber = findViewById(R.id.tv_channel_number)
        tvVolume = findViewById(R.id.tv_volume)
        tvPowerOn = findViewById(R.id.tv_is_power_on)
        tvCallCalling = findViewById(R.id.tv_call_calling)
        tvAmbulanceCalling = findViewById(R.id.tv_ambulance_calling)
        clChannelUp = findViewById(R.id.cl_channels_up)
        clChannelDown = findViewById(R.id.cl_channels_down)
        clVolumeUp = findViewById(R.id.cl_volume_up)
        clVolumeDown = findViewById(R.id.cl_volume_down)
        clCall = findViewById(R.id.cl_call)
        clAmbulance = findViewById(R.id.cl_ambulance)
        tvCallCalling.visibility = View.GONE
        tvAmbulanceCalling.visibility = View.GONE
        tvChannelNumber.text = getString(R.string.txt_channel_number, channelNumber)
        tvVolume.text = getString(R.string.txt_volume_count, volume)
        tvPowerOn.text = getString(R.string.txt_power_on)
        if (ContextCompat.checkSelfPermission(
                this@MainActivity, Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this@MainActivity, arrayOf(Manifest.permission.RECORD_AUDIO), 1
            )
        }
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this@MainActivity)
        val speechRecognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        clMic.setOnTouchListener(object : OnTouchListener {
            private val CLICK_ACTION_THRESHOLD = 200
            private var startX = 0f
            private var startY = 0f
            private var rect: Rect? = null
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                when (event?.action) {
                    MotionEvent.ACTION_DOWN -> {
                        rect = Rect(v!!.left, v.top, v.right, v.bottom)
                        startX = event.x;
                        startY = event.y;
                        handleMicBackground(true)
                        speechRecognizer.startListening(
                            speechRecognizerIntent
                        )
                    }
                    MotionEvent.ACTION_UP -> {
                        var endX = event.x
                        var endY = event.y
                        if (isAClick(startX, endX, startY, endY)) {
                            handleMicBackground(false)
                            speechRecognizer.stopListening()// WE HAVE A CLICK!!
                        }
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (!rect!!.contains(
                                v!!.left + event.x.toInt(), v.top + event.y
                                    .toInt()
                            )
                        ) {
                            // User moved outside bounds
                            handleMicBackground(false)
                            speechRecognizer.stopListening()
                        }
                    }
                }
                return true
            }

            private fun isAClick(startX: Float, endX: Float, startY: Float, endY: Float): Boolean {
                val differenceX = Math.abs(startX - endX)
                val differenceY = Math.abs(startY - endY)
                return !(differenceX > CLICK_ACTION_THRESHOLD /* =5 */ || differenceY > CLICK_ACTION_THRESHOLD)
            }

        })
        clChannelUp.setOnClickListener {
            handleChannelUp()
        }
        clChannelDown.setOnClickListener {
            handleChannelDown()
        }
        clVolumeUp.setOnClickListener {
            handleVolumeUp()
        }
        clVolumeDown.setOnClickListener {
            handleVolumeDown()
        }
        clCall.setOnClickListener {
            handleCallButton()
        }
        clAmbulance.setOnClickListener {
            handleAmbulanceButton()
        }
        clPower.setOnClickListener {
            if (isPowerOn) {
                handlePowerButtonChanges(false)
            } else {
                handlePowerButtonChanges(true)
            }
        }

        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(p0: Bundle?) {
                Log.d(TAG, "onReadyForSpeech")
            }

            override fun onBeginningOfSpeech() {
                Log.d(TAG, "onBeginningOfSpeech")
            }

            override fun onRmsChanged(p0: Float) {
                Log.d(TAG, "onRmsChanged")
            }

            override fun onBufferReceived(p0: ByteArray?) {
                Log.d(TAG, "onBufferReceived")
            }

            override fun onEndOfSpeech() {
                Log.d(TAG, "onEndOfSpeech")
                handleMicBackground(false)
                clearEditText()
            }

            @SuppressLint("SwitchIntDef")
            override fun onError(p0: Int) {
                Log.d(TAG, "onError $p0")
                when (p0) {
                    1 -> Toast.makeText(this@MainActivity, "Network Timeout", Toast.LENGTH_SHORT)
                        .show()
                    2 -> Toast.makeText(this@MainActivity, "Error Network", Toast.LENGTH_SHORT)
                        .show()
                    3 -> Toast.makeText(this@MainActivity, "Error Audio", Toast.LENGTH_SHORT).show()
                    4 -> Toast.makeText(this@MainActivity, "Error Server", Toast.LENGTH_SHORT)
                        .show()
                    5 -> println("Error Client")
                    6 -> Toast.makeText(this@MainActivity, "Speech Timeout", Toast.LENGTH_SHORT)
                        .show()
                    7 -> Toast.makeText(this@MainActivity, "No match found", Toast.LENGTH_SHORT)
                        .show()
                    8 -> Toast.makeText(this@MainActivity, "Recognizer busy", Toast.LENGTH_SHORT)
                        .show()
                    9 -> Toast.makeText(
                        this@MainActivity, "Insufficient permissions", Toast.LENGTH_SHORT
                    ).show()
                    10 -> Toast.makeText(
                        this@MainActivity, "Too many requests to handle", Toast.LENGTH_SHORT
                    ).show()
                    11 -> Toast.makeText(
                        this@MainActivity, "Server Disconnected", Toast.LENGTH_SHORT
                    ).show()
                    12 -> Toast.makeText(
                        this@MainActivity, "Language not supported", Toast.LENGTH_SHORT
                    ).show()
                    13 -> Toast.makeText(
                        this@MainActivity, "Language Unavailable", Toast.LENGTH_SHORT
                    ).show()
                    14 -> Toast.makeText(
                        this@MainActivity, "Cannot check support", Toast.LENGTH_SHORT
                    ).show()
                    else -> Toast.makeText(
                        this@MainActivity, "Unknown error occurred", Toast.LENGTH_SHORT
                    ).show()
                }
                handleMicBackground(false)
                clearEditText()
            }

            override fun onResults(p0: Bundle?) {
                Log.d(TAG, "onResults")
                val data = p0?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val stringData = (data?.get(0) ?: "").lowercase()
                etSpeechText.text = (stringData).toEditable()
                processData(stringData)
                Toast.makeText(this@MainActivity, "Listening Ended", Toast.LENGTH_SHORT).show()
                handleMicBackground(false)
                clearEditText()
            }

            override fun onPartialResults(p0: Bundle?) {
                Log.d(TAG, "onPartialResults")
                handleMicBackground(false)
                clearEditText()
            }

            override fun onEvent(p0: Int, p1: Bundle?) {
                Log.d(TAG, "onEvent")
            }
        })
    }

    override fun onStart() {
        super.onStart()
        if (ContextCompat.checkSelfPermission(
                this@MainActivity, Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this@MainActivity, arrayOf(Manifest.permission.RECORD_AUDIO), 1
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this@MainActivity, "Permission Granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@MainActivity, "Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleMicBackground(isMicON: Boolean) {
        if (isMicON) {
            clMic.setBackgroundColor(resources.getColor(R.color.white))
        } else {
            clMic.setBackgroundColor(resources.getColor(R.color.orange))
        }
    }

    private fun clearEditText() {
        Handler(Looper.getMainLooper()).postDelayed({
            etSpeechText.text.clear()
        }, 2000)
    }

    private fun processData(data: String) {
        if (data.contains("channel")) {
            if (isUp(data)) {
                handleChannelUp()
            } else if (isDown(data) && channelNumber > 0) {
                handleChannelDown()
            } else {
                Toast.makeText(
                    this@MainActivity, "Invalid input for channel action", Toast.LENGTH_SHORT
                ).show()
            }
        } else if (data.contains("volume")) {
            if (isUp(data)) {
                handleVolumeUp()
            } else if (isDown(data) && volume > 0) {
                handleVolumeDown()
            } else {
                Toast.makeText(
                    this@MainActivity, "Invalid input for volume action", Toast.LENGTH_SHORT
                ).show()
            }
        } else if (data.contains("power")) {
            if (data.contains("on")) {
                handlePowerButtonChanges(true)
            } else if (data.contains("off")) {
                handlePowerButtonChanges(false)
            } else {
                Toast.makeText(
                    this@MainActivity, "Invalid input for power action", Toast.LENGTH_SHORT
                ).show()
            }
        } else if (data.contains("call") && (!data.contains("ambulance") && !data.contains("emergency"))) {
            handleCallButton()
        } else if (data.contains("call") || data.contains("ambulance") || data.contains("emergency")) {
            handleAmbulanceButton()
        } else {
            Toast.makeText(this@MainActivity, "Invalid input", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isUp(data: String): Boolean {
        for (s: String in upKeywords) {
            if (data.contains(s, true)) {
                return true
            }
        }
        return false
    }

    private fun isDown(data: String): Boolean {
        for (s: String in downKeywords) {
            if (data.contains(s, true)) {
                return true
            }
        }
        return false
    }

    private fun handleChannelUp() {
        channelNumber++
        tvChannelNumber.text = getString(R.string.txt_channel_number, channelNumber)
        clChannelUp.setBackgroundColor(resources.getColor(R.color.white))
        Handler(Looper.getMainLooper()).postDelayed({
            clChannelUp.setBackgroundColor(resources.getColor(R.color.purple_200))
        }, 100)
        Toast.makeText(this@MainActivity, "Channel changed", Toast.LENGTH_SHORT).show()
    }

    private fun handleChannelDown() {
        if (channelNumber > 0) {
            channelNumber--
            tvChannelNumber.text = getString(R.string.txt_channel_number, channelNumber)
            clChannelDown.setBackgroundColor(resources.getColor(R.color.white))
            Handler(Looper.getMainLooper()).postDelayed({
                clChannelDown.setBackgroundColor(resources.getColor(R.color.purple_200))
            }, 100)
            Toast.makeText(this@MainActivity, "Channel changed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleVolumeUp() {
        volume++
        tvVolume.text = getString(R.string.txt_volume_count, volume)
        clVolumeUp.setBackgroundColor(resources.getColor(R.color.white))
        Handler(Looper.getMainLooper()).postDelayed({
            clVolumeUp.setBackgroundColor(resources.getColor(R.color.cream))
        }, 100)
        Toast.makeText(this@MainActivity, "Volume changed", Toast.LENGTH_SHORT).show()
    }

    private fun handleVolumeDown() {
        if (volume > 0) {
            volume--
            tvVolume.text = getString(R.string.txt_volume_count, volume)
            clVolumeDown.setBackgroundColor(resources.getColor(R.color.white))
            Handler(Looper.getMainLooper()).postDelayed({
                clVolumeDown.setBackgroundColor(resources.getColor(R.color.cream))
            }, 100)
            Toast.makeText(this@MainActivity, "Volume changed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handlePowerButtonChanges(isPowerON: Boolean) {
        if (isPowerON) {
            clPower.setBackgroundColor(resources.getColor(R.color.pink))
            tvPowerOn.text = getString(R.string.txt_power_on)
        } else {
            clPower.setBackgroundColor(resources.getColor(R.color.white))
            tvPowerOn.text = getString(R.string.txt_power_off)
        }
        isPowerOn = isPowerON
    }

    private fun handleCallButton() {
        if (!isCalling) {
            isCalling = true
            tvCallCalling.visibility = View.VISIBLE
            clCall.setBackgroundColor(resources.getColor(R.color.white))
            Handler(Looper.getMainLooper()).postDelayed({
                isCalling = false
                tvCallCalling.visibility = View.GONE
                clCall.setBackgroundColor(resources.getColor(R.color.yellow))
            }, 3000)
        } else {
            Toast.makeText(this@MainActivity, "Call Waiting", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleAmbulanceButton() {
        if (!isCallingAmbulance) {
            isCallingAmbulance = true
            tvAmbulanceCalling.visibility = View.VISIBLE
            clAmbulance.setBackgroundColor(resources.getColor(R.color.white))
            Handler(Looper.getMainLooper()).postDelayed({
                isCallingAmbulance = false
                tvAmbulanceCalling.visibility = View.GONE
                clAmbulance.setBackgroundColor(resources.getColor(R.color.red))
            }, 3000)
        } else {
            Toast.makeText(this@MainActivity, "Call Waiting", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val TAG: String = "MainActivity"
    }
}