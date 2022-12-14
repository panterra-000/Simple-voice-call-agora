package mapp.voyo.simple_voice_call_agora

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.agora.rtc2.*

class MainActivity : ComponentActivity() {

    // UI elements
    private var infoText: TextView? = null
    private var joinLeaveButton: Button? = null
    private var isJoined = false

    // Fill the App ID of your project generated on Agora Console.
    private val appId = "25846f242a2b46d3b468a0d2770e0a9b"

    // Fill the channel name.
    private val channelName = "NameUz"

    // Fill the temp token generated on Agora Console.
    private val token = "007eJxTYOj+o+acXNyV+K8+d2bFrWnPtmVeT8vNT2Wd8mpZiexVLm4FBiNTCxOzNCMTo0SjJBOzFGMgYZFokGJkbm6QapBomRRUOSO5IZCRYemNXGZGBggE8dkY/BJzU0OrGBgAvaIgjA=="

//    private val token = "007eJxTYAjkm8ET5/78vbRmrjvHpa6AbbYB7cYbH4XZzioVY/gm9kOBwcjUwsQszcjEKNEoycQsxRhIWCQapBiZmxukGiRaJh2unZHcEMjIsLQ0jZWRAQJBfDYGv8Tc1NAqBgYAum0dzA=="

    // An integer that identifies the local user.
    private val uid = 0

    // Agora engine instance
    private var agoraEngine: RtcEngine? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // If all the permissions are granted, initialize the RtcEngine object and join a channel.
        if (!checkSelfPermission()) {
            ActivityCompat.requestPermissions(this, REQUESTED_PERMISSIONS, PERMISSION_REQ_ID)
        }
        setupVoiceSDKEngine()

        // Set up access to the UI elements
        joinLeaveButton = findViewById(R.id.joinLeaveButton)
        infoText = findViewById(R.id.infoText)

    }


    private val PERMISSION_REQ_ID = 22
    private val REQUESTED_PERMISSIONS = arrayOf(
        Manifest.permission.RECORD_AUDIO
    )

    private fun checkSelfPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            REQUESTED_PERMISSIONS[0]
        ) == PackageManager.PERMISSION_GRANTED
    }


    fun showMessage(message: String?) {
        runOnUiThread {
            Toast.makeText(
                applicationContext,
                message,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private val mRtcEventHandler: IRtcEngineEventHandler = object : IRtcEngineEventHandler() {
        // Listen for the remote user joining the channel.
        override fun onUserJoined(uid: Int, elapsed: Int) {
            runOnUiThread { infoText!!.text = "Remote user joined: $uid" }
        }

        override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
            // Successfully joined a channel
            isJoined = true
            showMessage("Joined Channel $channel")
            runOnUiThread { infoText!!.text = "Waiting for a remote user to join" }
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            // Listen for remote users leaving the channel
            showMessage("Remote user offline $uid $reason")
            if (isJoined) runOnUiThread { infoText!!.text = "Waiting for a remote user to join" }
        }

        override fun onLeaveChannel(stats: RtcStats) {
            // Listen for the local user leaving the channel
            runOnUiThread { infoText!!.text = "Press the button to join a channel" }
            isJoined = false
        }
    }

    private fun setupVoiceSDKEngine() {
        try {
            val config = RtcEngineConfig()
            config.mContext = baseContext
            config.mAppId = appId
            config.mEventHandler = mRtcEventHandler

            agoraEngine = RtcEngine.create(config)

        } catch (e: Exception) {
            throw RuntimeException("Check the error.")
        }
    }

    private fun joinChannel() {
        val options = ChannelMediaOptions()
        options.autoSubscribeAudio = true
        // Set both clients as the BROADCASTER.
        options.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
        // Set the channel profile as BROADCASTING.
        options.channelProfile = Constants.CHANNEL_PROFILE_LIVE_BROADCASTING

        // Join the channel with a temp token.
        // You need to specify the user ID yourself, and ensure that it is unique in the channel.
        agoraEngine!!.joinChannel(token, channelName, uid, options)
    }


    fun joinLeaveChannel(view: View?) {
        if (isJoined) {
            agoraEngine!!.leaveChannel()
            joinLeaveButton!!.text = "Join"
        } else {
            joinChannel()
            joinLeaveButton!!.text = "Leave"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        agoraEngine!!.leaveChannel()

        // Destroy the engine in a sub-thread to avoid congestion
        Thread {
            RtcEngine.destroy()
            agoraEngine = null
        }.start()
    }
}

